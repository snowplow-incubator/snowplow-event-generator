/*
 * Copyright (c) 2021-2025 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.eventgen.sinks

import java.nio.file.{Paths => JPaths}
import java.net.URI

import fs2.{Chunk, Pipe, Pull, Stream}
import fs2.io.file.{Flags, Path}
import fs2.io.file.Files.forAsync
import fs2.compression.Compression.forSync
import fs2.concurrent.Channel
import fs2.text.{base64, utf8}

import cats.syntax.all._

import cats.effect.kernel.{Async, Sync}

import blobstore.s3.S3Store
import blobstore.url.Url

import software.amazon.awssdk.services.s3.S3AsyncClient

import com.snowplowanalytics.snowplow.analytics.scalasdk.Event

import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest
import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload
import com.snowplowanalytics.snowplow.eventgen.Config

object File {

  def make[F[_]: Async](config: Config, fileConfig: Config.Output.File) = new Sink[F] {

    override def collectorPayload: Pipe[F, CollectorPayload, Unit] =
      pipe(
        config.payloadsPerFile,
        rawSerializer(config.compress),
        mkWriter(config, fileConfig, "raw")
      )

    override def enriched: Pipe[F, Event, Unit] =
      if (config.withEnrichedTsv)
        pipe(
          config.payloadsPerFile,
          enrichedTsvSerializer(config.compress),
          mkWriter(config, fileConfig, "enriched-tsv")
        )
      else if (config.withEnrichedJson)
        pipe(
          config.payloadsPerFile,
          enrichedJsonSerializer(config.compress),
          mkWriter(config, fileConfig, "enriched-json")
        )
      else
        _ => Stream.raiseError(new IllegalArgumentException(s"Can't determine enriched format to use"))

    override def http: Pipe[F, HttpRequest, Unit] =
      _ => Stream.raiseError(new IllegalStateException(s"Can't use file output for HTTP requests"))
  }

  private def pipe[F[_]: Async, A](
    nbEventsPerFile: Int,
    serializer: Pipe[F, A, Byte],
    writer: Int => Pipe[F, Byte, Unit]
  ): Pipe[F, A, Unit] =
    rotate(nbEventsPerFile) { idx =>
      _.through(serializer).through(writer(idx))
    }

  /* A Pipe that periodically rotates the inner pipe it sends elements through
   *
   * There is only ever one active inner pipe. Each element is sent through exactly one inner pipe.
   * Each inner pipe receives at most `max` elements
   *
   * @param max The maximum nuber of elements to send through an inner pipe
   * @param toPipe Generates a new inner pipe when one is needed. It is called with an integer
   * representing an increasing 1-based pipe index.
   */
  private def rotate[F[_]: Async, A](max: Int)(toPipe: Int => Pipe[F, A, Unit]): Pipe[F, A, Unit] = {
    def newChannel = Channel.synchronous[F, Chunk[A]]

    // Adds sub-streams to the outputs channels, with each sub-stream receiving `max` elements
    def go(
      in: Stream[F, A],
      aCount: Int,
      streamCount: Int,
      outputs: Channel[F, (Int, Stream[F, A])],
      chan: Channel[F, Chunk[A]]
    ): Pull[F, Nothing, Unit] =
      in.pull.uncons.flatMap {
        case None => Pull.eval(outputs.close *> chan.close) *> Pull.done
        case Some((chunk, rest)) =>
          val (split1, split2) = chunk.splitAt(max - aCount)
          for {
            streamCount <-
              if (aCount == 0)
                Pull.eval {
                  outputs.send((streamCount, chan.stream.unchunks)).as(streamCount + 1)
                }
              else Pull.pure(streamCount)
            _      <- Pull.eval(chan.send(split1))
            aCount <- Pull.pure(aCount + split1.size)
            chan   <- if (aCount >= max) Pull.eval(chan.close *> newChannel) else Pull.pure(chan)
            aCount <- if (aCount >= max) Pull.pure(0) else Pull.pure(aCount)
            _      <- go(Stream.chunk(split2) ++ rest, aCount, streamCount, outputs, chan)
          } yield ()
      }

    in =>
      (for {
        outputs <- Stream.eval(Channel.synchronous[F, (Int, Stream[F, A])])
        current <- Stream.eval(newChannel)
      } yield {
        // s1 is a stream that adds sub-streams and their index to the synchrnous outputs channel
        val s1 = go(in, 0, 0, outputs, current).stream

        // s2 is a stream that receives the substreams and sends them through the inner pipes
        val s2 = outputs.stream.flatMap { case (idx, stream) => stream.through(toPipe(idx + 1)) }

        // Run them concurrently
        s1.merge(s2).drain.void
      }).flatMap(identity[Stream[F, Unit]])
  }

  private def mkWriter[F[_]: Async](
    config: Config,
    fileConfig: Config.Output.File,
    prefix: String
  ): Int => Pipe[F, Byte, Unit] = { idx =>
    val uri    = fileConfig.path
    val suffix = if (config.compress) ".gz" else ""

    if (uri.toString.startsWith("s3:")) {
      s3(prefix, suffix, idx, uri)
    } else if (uri.toString.startsWith("file:")) {
      file(prefix, suffix, idx, uri)
    } else { _ =>
      Stream.raiseError(new IllegalArgumentException(s"Unknown scheme in $uri for file output"))
    }
  }

  private def s3[F[_]: Async](prefix: String, suffix: String, idx: Int, baseDir: URI): Pipe[F, Byte, Unit] =
    in =>
      Stream.eval(Url.parseF[F](s"$baseDir/$prefix/${prefix}_${pad(idx)}$suffix")).flatMap { url =>
        val store = S3Store[F](S3AsyncClient.builder().build())
        in.through(store.put(url, overwrite = true, None, None))
      }

  private def file[F[_]: Async](prefix: String, suffix: String, idx: Int, baseDir: URI): Pipe[F, Byte, Unit] = { in =>
    val catDir = Path.fromNioPath(JPaths.get(baseDir).resolve(prefix))
    Stream.eval(forAsync[F].createDirectories(catDir)) *>
      in.through(forAsync[F].writeAll(catDir.resolve(s"${prefix}_${pad(idx)}$suffix"), Flags.Write))
  }

  private def rawSerializer[F[_]: Sync](compress: Boolean): Pipe[F, CollectorPayload, Byte] =
    _.flatMap(e => Stream.emits(e.toRaw)).through(base64.encode).through(stringSerializer(compress))

  private def enrichedTsvSerializer[F[_]: Sync](compress: Boolean): Pipe[F, Event, Byte] =
    _.map(_.toTsv).through(stringSerializer(compress))

  private def enrichedJsonSerializer[F[_]: Sync](compress: Boolean): Pipe[F, Event, Byte] =
    _.map(_.toJson(true).noSpaces).through(stringSerializer(compress))

  private def stringSerializer[F[_]: Sync](compress: Boolean): Pipe[F, String, Byte] = {
    val pipe: Pipe[F, String, Byte] = _.flatMap(x => Stream(x, "\n")).through(utf8.encode)
    if (compress)
      pipe.andThen(forSync[F].gzip())
    else
      pipe
  }

  private def pad(idx: Int): String = f"$idx%04d"
}

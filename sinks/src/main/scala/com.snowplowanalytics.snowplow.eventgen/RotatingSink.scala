/*
 * Copyright (c) 2021-2022 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.eventgen

import fs2.{Chunk, Pipe, Pull, Stream}
import fs2.io.file.{Files, Flags, Path}
import fs2.concurrent.Channel

import java.nio.file.{Paths => JPaths}
import java.net.URI
import blobstore.s3.S3Store
import blobstore.url.Url
import cats.effect.Async
import cats.syntax.all._
import software.amazon.awssdk.services.s3.S3AsyncClient

object RotatingSink {

  /* A Pipe that periodically rotates the inner pipe it sends elements through
   *
   * There is only ever one active inner pipe. Each element is sent through exactly one inner pipe.
   * Each inner pipe receives at most `max` elements
   *
   * @param max The maximum nuber of elements to send through an inner pipe
   * @param toPipe Generates a new inner pipe when one is needed. It is called with an integer
   * representing an increasing 1-based pipe index.
   */
  def rotate[F[_]: Async, A](max: Int)(toPipe: Int => Pipe[F, A, Nothing]): Pipe[F, A, Nothing] = {
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
        s1.merge(s2)
      }).flatMap(identity[Stream[F, Nothing]])
  }

  def s3[F[_]: Async](prefix: String, suffix: String, idx: Int, baseDir: URI): Pipe[F, Byte, Nothing] =
    in =>
      Stream
        .eval(Url.parseF[F](s"$baseDir/$prefix/${prefix}_${pad(idx)}$suffix"))
        .flatMap { url =>
          val store = S3Store[F](S3AsyncClient.builder().build())
          in.through(store.put(url, overwrite = true, None, None))
        }
        .drain

  def file[F[_]: Async](prefix: String, suffix: String, idx: Int, baseDir: URI): Pipe[F, Byte, Nothing] = { in =>
    val catDir = Path.fromNioPath(JPaths.get(baseDir).resolve(prefix))
    Stream.eval(Files[F].createDirectories(catDir)) *>
      in.through(Files[F].writeAll(catDir.resolve(s"${prefix}_${pad(idx)}$suffix"), Flags.Write))
  }

  private def pad(idx: Int): String = f"$idx%10d"
}

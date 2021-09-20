/*
 * Copyright (c) 2021 Snowplow Analytics Ltd. All rights reserved.
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

import java.nio.file.{ Paths, Path }
import java.net.URI

import scala.util.Random

import fs2.{ Stream, Pipe }
import fs2.compression.gzip
import fs2.io.file.{createDirectory, writeAll}

import cats.implicits._
import cats.effect.{Blocker, Sync, Timer, ConcurrentEffect, ContextShift}
import cats.effect.concurrent.Ref

import blobstore.s3.S3Store
import blobstore.url.Url

import software.amazon.awssdk.services.s3.S3AsyncClient

import com.snowplowanalytics.snowplow.analytics.scalasdk.Event


object Sink {

  type FileName = String

  type Sink[F[_]] = FileName => Pipe[F, Byte, Unit]

  /** Create a stream of events for a particular file */
  def createFileStream[F[_]: Sync](config: Config, getName: F[String], totalAfter: Ref[F, Int], duplicates: Ref[F, Duplicates.State]): F[(Int, String, Stream[F, Event])] =
    for {
      perFile  <- Sync[F].delay(Random.between(config.minPerFile, config.maxPerFile))
      totalNow <- totalAfter.updateAndGet(_ + perFile)
      name     <- getName
    } yield (totalNow, name, EnrichedEventGen.eventStream(config, duplicates).take(perFile.toLong))


  def getSink[F[_]: ConcurrentEffect: ContextShift: Timer](blocker: Blocker, path: URI): Sink[F] =
    (fileName: FileName) =>
      if (path.toString.startsWith("file:")) {
          (in: Stream[F, Byte]) => {
            val mkDir = createDirectory[F](blocker, Path.of(path)).attempt.as(Path.of(path))
            Stream.eval(mkDir).flatMap { dir =>
              in.through(writeAll[F](Paths.get(s"$dir/$fileName"), blocker))
            }
          }
      } else if (path.toString.startsWith("s3:")) {
        val store = S3Store[F](S3AsyncClient.builder().build())
          (in: Stream[F, Byte]) => 
            Stream.eval(Url.parseF[F](s"$path/$fileName")).flatMap { url =>
              in.through(store.put(url, true, None, None))
            }.onFinalize(ConcurrentEffect[F].delay(println(s"Done with $fileName")))
      } else _ => Stream.raiseError[F](new RuntimeException(s"Unknown scheme in $path"))


  def run[F[_]: ConcurrentEffect: ContextShift: Timer](dir: URI, cfg: Config): F[Unit] =
    (Duplicates.pregeneratePairs[F](cfg.duplicates.totalDupes), Blocker[F]).tupled.use {
      case (duplicates, blocker) =>
        for {
          cpus         <- Sync[F].delay(Runtime.getRuntime.availableProcessors)  // RNG is CPU-bound
          sinkBuilder   = getSink[F](blocker, dir)
          fileCounter  <- Ref.of[F, Int](0)
          totalCounter <- Ref.of[F, Int](0)     // Every stream before starting increases this with 
                                                // an amount of events it'd like to write
          getName       = fileCounter.updateAndGet(_ + 1).map(pad).map(i => s"part-$i.gz")
          sink          = Stream.repeatEval(createFileStream(cfg, getName, totalCounter, duplicates))
                                .collectWhile { 
                                  case (cardinality, path, stream) if cardinality < cfg.total =>
                                    stream.map(_.toTsv)
                                          .intersperse("\n")
                                          .through(fs2.text.utf8Encode)
                                          .through(gzip[F]())
                                          .through(sinkBuilder(path))
                                }
                                .parJoin(cpus)
          _            <- sink.compile.drain
        } yield ()
    }


  private def pad(int: Int): String = {
    val zeros = "0".repeat(4 - int.toString.length)
    s"$zeros$int"
  }
}


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

import fs2.{INothing, Pipe, Stream}
import cats.effect.kernel.Sync
import cats.effect.{Async, ExitCode, IO, IOApp}
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent

import java.net.URI


object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Config.parse(args) match {
      case Right(Config.Cli(config, outputUri)) =>
          sink[IO](outputUri, config).as(ExitCode.Success)
      case Left(error) =>
        IO(System.err.println(error)).as(ExitCode.Error)

    }

  type GenOutput = (collector.CollectorPayload, List[Event])


  def sink[F[_] : Async](outputDir: URI, config: Config): F[Unit] = {
    val rng = new scala.util.Random(config.seed)

    val eventStream: Stream[F, GenOutput] = {
      config.duplicates match {
        case Some(dups) => Stream.repeatEval(Sync[F].delay(
          runGen(SdkEvent.genPairDup(dups.natProb, dups.synProb, dups.natTotal, dups.synTotal, config.eventPerPayloadMin, config.eventPerPayloadMax), rng)))
        case None => Stream.repeatEval(Sync[F].delay(runGen(SdkEvent.genPair(config.eventPerPayloadMin, config.eventPerPayloadMax), rng)))
      }
    }

    def sinkBuilder(prefix: String, idx: Int): Pipe[F, Byte, INothing] = {
      val suffix = if (config.compress) ".gz" else ""
      if (outputDir.toString.startsWith("file:")) {
        RotatingSink.file(prefix, suffix, idx, outputDir)
      } else if (outputDir.toString.startsWith("s3:")) {
        RotatingSink.s3(prefix, suffix, idx, outputDir)
      } else {
        throw new RuntimeException(s"Unknown scheme in $outputDir")
      }
    }

    def sinkFor(name: String, idx: Int, predicate: Boolean): Pipe[F, Byte, INothing] =
      if (predicate) sinkBuilder(name, idx)
      else _.drain

    eventStream
      .take(config.payloadsTotal.toLong)
      .through(RotatingSink.rotate(config.payloadsPerFile) { idx =>
        val pipe1: Pipe[F, GenOutput, INothing]  = _.map(_._1)
          .through(Serializers.rawSerializer(config.compress))
          .through(sinkFor("raw", idx, config.withRaw))
        val pipe2: Pipe[F, GenOutput, INothing]  = _.flatMap(in => Stream.emits(in._2))
          .through(Serializers.enrichedTsvSerializer(config.compress))
          .through(sinkFor("enriched", idx, config.withEnrichedTsv))
        val pipe3: Pipe[F, GenOutput, INothing]  = _.flatMap(in => Stream.emits(in._2))
          .through(Serializers.enrichedJsonSerializer(config.compress))
          .through(sinkFor("transformed", idx, config.withEnrichedJson))
        in: Stream[F, GenOutput] => in.broadcastThrough(pipe1, pipe2, pipe3)
      })
      .compile
      .drain
  }

}

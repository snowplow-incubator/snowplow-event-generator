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

import fs2.{INothing, Pipe, Stream}
import cats.effect.kernel.Sync
import cats.effect.{Async, ExitCode, IO, IOApp}
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.eventgen.Config.EnrichFormat
import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent

import java.net.URI


object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Config.application.parse(args) match {
      case Right(Config.Cli(configPath, outputUri)) =>
        Config.fromPath[IO](configPath.toString).flatMap {
          case Right(config) => sink[IO](outputUri, config).as(ExitCode.Success)
          case Left(error) => IO(System.err.println(error)).as(ExitCode.Error)
        }
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

    def sinkBuilder(prefix: String, idx: Int): Pipe[F, Byte, INothing] =
      if (outputDir.toString.startsWith("file:")) {
        RotatingSink.file(prefix, idx, outputDir)
      } else if (outputDir.toString.startsWith("s3:")) {
        RotatingSink.s3(prefix, idx, outputDir)
      } else {
        throw new RuntimeException(s"Unknown scheme in $outputDir")
      }

    def rawSink(idx: Int): Pipe[F, Byte, INothing] =
      if (config.withRaw)
        sinkBuilder("raw", idx)
      else
        _.drain

    def enrichSink(idx: Int): Pipe[F, Byte, INothing] =
      config.enrichFormat match {
        case EnrichFormat.Json => sinkBuilder("json-enrich", idx)
        case EnrichFormat.Tsv => sinkBuilder("tsv-enrich", idx)
      }

    eventStream
      .take(config.payloadsTotal.toLong)
      .through(RotatingSink.rotate(config.payloadsPerFile) { idx =>
        val pipe1: Pipe[F, GenOutput, INothing]  = _.map(_._1)
          .through(Serializers.rawSerializer(config.compress))
          .through(rawSink(idx))
        val pipe2: Pipe[F, GenOutput, INothing]  = _.flatMap(in => Stream.emits(in._2))
          .through(Serializers.enrichedSerializer(config.enrichFormat, config.compress))
          .through(enrichSink(idx))
        in: Stream[F, GenOutput] => in.broadcastThrough(pipe1, pipe2)
      })
      .compile
      .drain
  }

}

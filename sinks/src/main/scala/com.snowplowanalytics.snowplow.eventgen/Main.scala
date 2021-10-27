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

import fs2.Stream
import cats.implicits._
import cats.effect.kernel.Sync
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
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


  def sink[F[_] : Async](outputDir: URI, config: Config): F[Unit] = {
    val rng = new scala.util.Random(config.seed)

    val eventStream: Stream[F, (collector.CollectorPayload, List[Event])] = {
      config.duplicates match {
        case Some(dups) => Stream.repeatEval(Sync[F].delay(
          runGen(SdkEvent.genPairDup(dups.natProb, dups.synProb, dups.natTotal, dups.synTotal, config.eventPerPayloadMin, config.eventPerPayloadMax), rng)))
        case None => Stream.repeatEval(Sync[F].delay(runGen(SdkEvent.genPair(config.eventPerPayloadMin, config.eventPerPayloadMax), rng)))
      }
    }

    def sinkBuilder(prefix: String) = if (outputDir.toString.startsWith("file:")) {
      RotatingSink.file(prefix, outputDir, config.payloadsPerFile)
    } else if (outputDir.toString.startsWith("s3:")) {
      RotatingSink.s3(prefix, outputDir, config.payloadsPerFile)
    } else {
      throw new RuntimeException(s"Unknown scheme in $outputDir")
    }

    val rawSink: Resource[F, RotatingSink[F]] = if (config.withRaw)
      sinkBuilder("raw")
    else
      RotatingSink.void

    val enrichSink: Resource[F, RotatingSink[F]] = if (config.withRaw)
      config.enrichFormat match {
        case EnrichFormat.Json => sinkBuilder("json-enrich")
        case EnrichFormat.Tsv => sinkBuilder("tsv-enrich")
      }
    else RotatingSink.void


    (for {
      raw <- rawSink
      enrich <- enrichSink
    } yield (raw, enrich)).use { case (raw, enrich) =>
      eventStream
        .zipWithIndex
        .takeWhile(_._2 < config.payloadsTotal)
        .map(_._1)
        .evalTap {
          case (payload, sdkEvents) =>
            raw.write(Stream.emit(payload).through(Serializers.rawSerializer(config.compress))) *>
              enrich.write(Stream.emits(sdkEvents).through(Serializers.enrichedSerializer(config.enrichFormat, config.compress)))
        }
        .compile
        .drain
    }
  }

}

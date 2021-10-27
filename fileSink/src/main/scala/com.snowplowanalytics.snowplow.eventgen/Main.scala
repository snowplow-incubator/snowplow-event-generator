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

import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import cats.implicits._
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.eventgen.Config.EnrichFormat
import fs2.Stream
import java.net.URI


object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Config.application.parse(args) match {
      case Right(Config.Cli(configPath, outputUri)) =>
        Config.fromPath[IO](configPath.toString).flatMap {
          case Right(config) => sinkToFile[IO](outputUri, config).as(ExitCode.Success)
          case Left(error) => IO(System.err.println(error)).as(ExitCode.Error)
        }
      case Left(error) =>
        IO(System.err.println(error)).as(ExitCode.Error)

    }

  def sinkToFile[F[_] : Async](outputDir: URI, config: Config): F[Unit] = {
    val eventStream: Stream[F, (collector.CollectorPayload, List[Event])] = EventStream(config.seed).makeStreamPair

    val rawSink: Resource[F, RotatingFileSink[F]] = if (config.withRaw)
      RotatingFileSink("raw", outputDir, config.eventsPerFile)
    else
      RotatingFileSink.void

    val enrichSink: Resource[F, RotatingFileSink[F]] = if (config.withRaw)
      config.enrichFormat match {
        case EnrichFormat.Json => RotatingFileSink[F]("json-enrich", outputDir, config.eventsPerFile)
        case EnrichFormat.Tsv => RotatingFileSink[F]("tsv-enrich", outputDir, config.eventsPerFile)
      }
    else RotatingFileSink.void


    (for {
      raw <- rawSink
      enrich <- enrichSink
    } yield (raw, enrich)).use { case (raw, enrich) =>
      eventStream
        .zipWithScan(0) { case (counter, events) => events._2.length + counter }
        .takeWhile(_._2 < config.total)
        .map(_._1)
        .evalTap {
          case (payload, sdkEvents) =>
            raw.write(Stream.emit(payload).through(Serializers.rawSerializer)) *>
              enrich.write(Stream.emits(sdkEvents).through(Serializers.enrichedSerializer(config)))

        }
        .compile
        .drain
    }
  }

}

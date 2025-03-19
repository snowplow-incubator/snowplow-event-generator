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
package com.snowplowanalytics.snowplow.eventgen

import java.time.Instant

import scala.concurrent.duration.DurationInt

import org.scalacheck.{Gen => ScalaGen}

import fs2.{Pipe, Stream}

import cats.syntax.all._

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.{Async, Clock, Ref, Sync}

import com.snowplowanalytics.snowplow.eventgen.protocol.contexts.AllContexts
import com.snowplowanalytics.snowplow.eventgen.protocol.unstructs.AllUnstructs
import com.snowplowanalytics.snowplow.eventgen.protocol.SelfDescribingJsonGen
import com.snowplowanalytics.snowplow.eventgen.sinks.Sink
import com.snowplowanalytics.snowplow.eventgen.GenConfig
import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest
import com.snowplowanalytics.snowplow.eventgen.collector.Api

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Config.parse(args) match {
      case Right(Config.Cli(config)) =>
        def unstructCounts = printCounts(AllUnstructs.all)
        def contextCounts  = printCounts(AllContexts.all)
        generate[IO](config) >>
          IO.println(s"""Contexts:
                        |$contextCounts
                        |Unstruct Events:
                        |$unstructCounts
                        |""".stripMargin)
            .as(ExitCode.Success)
      case Left(error) =>
        IO(System.err.println(error)).as(ExitCode.Error)

    }

  private def printCounts(gens: List[SelfDescribingJsonGen]): String =
    gens.map(g => s"  ${g.schemaKey.toSchemaUri} = ${g.genCount}").mkString("\n")

  private def generate[F[_]: Async](config: Config): F[Unit] = {

    val sink: Sink[F] = Sink.make(config.output)

    config.events match {
      case GenConfig.Events.CollectorPayloads =>
        run(
          mkStream(config, Gen.collectorPayload(config, _)),
          sink.collectorPayload
        )
      case GenConfig.Events.Enriched(format, generateEnrichments) =>
        run(
          mkStream(config, Gen.enriched(config, _, format, generateEnrichments)).flatMap(Stream.emits),
          sink.enriched
        )
      case GenConfig.Events.Http(_) =>
        val smallBody = "a" * 1000
        val bigBody   = "a" * 200000
        val smallRequest = HttpRequest(
          HttpRequest.Method.Post(Api("com.snowplowanalytics.snowplow", "tp2")),
          Map.empty[String, String],
          None,
          Some(smallBody)
        )
        val bigRequest = HttpRequest(
          HttpRequest.Method.Post(Api("com.snowplowanalytics.snowplow", "tp2")),
          Map.empty[String, String],
          None,
          Some(bigBody)
        )

        val gen = ScalaGen.frequency(
          (config.bodySizesFrequencies.small, smallRequest),
          (config.bodySizesFrequencies.big, bigRequest)
        )
        run(
          mkStream(config, _ => gen),
          sink.http
        )
    }
  }

  def run[F[_]: Async, A](events: Stream[F, A], sink: Pipe[F, A, Unit]): F[Unit] =
    Stream
      .eval(Ref.of(0))
      .flatMap { counts =>
        val reportPeriod = 1.minute
        val showCounts = Stream.awakeEvery(reportPeriod).evalMap { _ =>
          counts
            .getAndSet(0)
            .flatMap(nbEvents => Sync[F].delay(println(s"$nbEvents events generated in the last $reportPeriod")))
        }

        events.through(sink).evalTap(_ => counts.update(_ + 1)).concurrently(showCounts)
      }
      .compile
      .drain

  private def mkStream[F[_]: Async, A](config: Config, mkGen: Instant => ScalaGen[A]): Stream[F, A] =
    for {
      rng <- Stream.emit {
        config.seed.fold(new scala.util.Random(scala.util.Random.nextInt()))(seed => new scala.util.Random(seed))
      }
      time <- Stream.eval {
        config.timestamp match {
          case Config.Timestamp.Now =>
            Clock[F].realTimeInstant.flatTap(t => Sync[F].delay(println(s"Timestamp used in events: $t")))
          case Config.Timestamp.Fixed(time) =>
            Async[F].pure(time).flatTap(t => Sync[F].delay(println(s"Timestamp used in events: $t")))
        }
      }
      events = Stream(1)
        .repeat
        .covary[F]
        .parEvalMap(Runtime.getRuntime.availableProcessors * 5)(_ => Sync[F].delay(runGen(mkGen(time), rng)))
      event <- config.eventsTotal.fold(events)(events.take)
    } yield event
}

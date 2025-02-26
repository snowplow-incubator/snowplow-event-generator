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

import org.scalacheck.{Gen => ScalaGen}

import fs2.{Pipe, Stream}

import cats.syntax.all._

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.{Async, Clock, Sync}

import com.snowplowanalytics.snowplow.eventgen.protocol.contexts.AllContexts
import com.snowplowanalytics.snowplow.eventgen.protocol.unstructs.AllUnstructs
import com.snowplowanalytics.snowplow.eventgen.protocol.SelfDescribingJsonGen
import com.snowplowanalytics.snowplow.eventgen.sinks.Sink

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Config.parse(args) match {
      case Right(Config.Cli(config)) =>
        val unstructCounts = printCounts(AllUnstructs.all)
        val contextCounts  = printCounts(AllContexts.all)
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

  def printCounts(gens: List[SelfDescribingJsonGen]): String =
    gens.map(g => s"  ${g.schemaKey.toSchemaUri} = ${g.genCount}").mkString("\n")

  def generate[F[_]: Async](config: Config): F[Unit] = {

    val sink: Sink[F] = Sink.make(config, config.output)

    if (config.withRaw) {
      // Collector payloads
      run(
        config.payloadsTotal.toLong,
        mkStream(config, Gen.collectorPayload(config, _)),
        sink.collectorPayload
      )
    } else if (config.withEnrichedTsv || config.withEnrichedJson) {
      // Enriched events
      run(
        config.payloadsTotal.toLong,
        mkStream(config, Gen.enriched(config, _)).flatMap(Stream.emits),
        sink.enriched
      )
    } else if (config.output.isInstanceOf[Config.Output.Http]) {
      // HTTP requests
      run(
        config.payloadsTotal.toLong,
        mkStream(config, Gen.httpRequest(config, _)),
        sink.http
      )
    } else {
      Sync[F].raiseError(
        new IllegalArgumentException("Can't determine the type of events to generate based on the configuration")
      )
    }
  }

  def run[F[_]: Sync, A](nbEvents: Long, events: Stream[F, A], sink: Pipe[F, A, Unit]): F[Unit] =
    events.take(nbEvents).through(sink).compile.drain

  def mkStream[F[_]: Async, A](config: Config, mkGen: Instant => ScalaGen[A]): Stream[F, A] =
    for {
      rng <- Stream.emit {
        if (config.randomisedSeed) new scala.util.Random(scala.util.Random.nextInt())
        else new scala.util.Random(config.seed)
      }
      time <- Stream.eval {
        config.timestamps match {
          case Config.Timestamps.Now => Clock[F].realTimeInstant.flatTap(t => Sync[F].delay(println(s"time: $t")))
          case Config.Timestamps.Fixed(time) => Async[F].pure(time).flatTap(t => Sync[F].delay(println(s"time: $t")))
        }
      }
      event <- Stream.repeatEval(Sync[F].delay(runGen(mkGen(time), rng)))
    } yield event
}

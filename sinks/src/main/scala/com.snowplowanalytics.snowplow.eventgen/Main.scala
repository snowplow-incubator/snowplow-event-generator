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
import cats.syntax.all._
import cats.effect.kernel.Sync
import cats.effect.{Async, Clock, ExitCode, IO, IOApp}
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent
import com.snowplowanalytics.snowplow.eventgen.protocol.Context
import com.snowplowanalytics.snowplow.eventgen.protocol.event.UnstructEvent
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest
import software.amazon.kinesis.common.KinesisClientUtil

import java.net.URI


object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Config.parse(args) match {
      case Right(Config.Cli(config, outputUri)) =>
        sink[IO](outputUri, config) >>
          IO.println(
            s"""changeFormGenCount  = ${Context.changeFormGenCount}
               |clientSessionGenCount  = ${Context.clientSessionGenCount}
               |consentDocumentCount  = ${Context.consentDocumentCount}
               |desktopContextCount  = ${Context.desktopContextCount}
               |httpCookieCount  = ${Context.httpCookieCount}
               |httpHeaderCount  = ${Context.httpHeaderCount}
               |googleCookiesCount  = ${Context.googleCookiesCount}
               |googlePrivateCount  = ${Context.googlePrivateCount}
               |optimizelyVisitorCount  = ${Context.optimizelyVisitorCount}
               |optimizelyStateCount  = ${Context.optimizelyStateCount}
               |optimizelyVariationCount  = ${Context.optimizelyVariationCount}
               |optimizelySummaryCount  = ${Context.optimizelySummaryCount}
               |sessionContextCount  = ${Context.sessionContextCount}
               |consentWithdrawnCount  = ${Context.consentWithdrawnCount}
               |segmentScreenCount  = ${Context.segmentScreenCount}
               |pushRegistrationCount  = ${Context.pushRegistrationCount}
               |uaParserContextCount  = ${Context.uaParserContextCount}
               |unstuctEventCount = ${UnstructEvent.unstuctEventCount}
               |""".stripMargin)
            .as(ExitCode.Success)
      case Left(error) =>
        IO(System.err.println(error)).as(ExitCode.Error)

    }

  type GenOutput = (collector.CollectorPayload, List[Event])


  def sink[F[_] : Async](outputDir: URI, config: Config): F[Unit] = {
    val rng = new scala.util.Random(config.seed)
    val timeF = config.timestamps match {
      case Config.Timestamps.Now => Clock[F].realTimeInstant flatMap (t => Sync[F].delay(println(s"time: $t")).as(t))
      case Config.Timestamps.Fixed(time) => Async[F].pure(time) flatMap (t => Sync[F].delay(println(s"time: $t")).as(t))
    }

    val eventStream: Stream[F, GenOutput] =
      Stream.eval(timeF).flatMap { time =>
        config.duplicates match {
          case Some(dups) => Stream.repeatEval(Sync[F].delay(
            runGen(SdkEvent.genPairDup(dups.natProb, dups.synProb, dups.natTotal, dups.synTotal, config.eventPerPayloadMin, config.eventPerPayloadMax, time), rng)))
          case None => Stream.repeatEval(Sync[F].delay(runGen(SdkEvent.genPair(config.eventPerPayloadMin, config.eventPerPayloadMax, time), rng)))
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

    def makeOutput: Pipe[F, GenOutput, Unit] =
      if (outputDir.toString.startsWith("kinesis:")) {
        if (List(config.withRaw, config.withEnrichedTsv, config.withEnrichedJson).count(identity) > 1)
          throw new RuntimeException(s"Kinesis could only output in single format")
        if (config.compress) {
          throw new RuntimeException(s"Kinesis doesn't support compression")
        }
        val kinesisClient: KinesisAsyncClient = KinesisClientUtil.createKinesisAsyncClient(KinesisAsyncClient.builder())

        def reqBuilder(event: Event): PutRecordRequest = PutRecordRequest.builder()
          .streamName(outputDir.getRawAuthority)
          .partitionKey(event.event_id.toString)
          .data(SdkBytes.fromUtf8String(event.toTsv))
          .build()

        st: Stream[F, GenOutput] =>
          st.map(_._2)
            .flatMap(Stream.emits)
            .map(reqBuilder)
            .parEvalMap(10)(e =>
              Async[F].fromCompletableFuture(Sync[F].delay(kinesisClient.putRecord(e)))
            ).void
      } else {
        RotatingSink.rotate(config.payloadsPerFile) { idx =>
          val pipe1: Pipe[F, GenOutput, INothing] = _.map(_._1)
            .through(Serializers.rawSerializer(config.compress))
            .through(sinkFor("raw", idx, config.withRaw))
          val pipe2: Pipe[F, GenOutput, INothing] = _.flatMap(in => Stream.emits(in._2))
            .through(Serializers.enrichedTsvSerializer(config.compress))
            .through(sinkFor("enriched", idx, config.withEnrichedTsv))
          val pipe3: Pipe[F, GenOutput, INothing] = _.flatMap(in => Stream.emits(in._2))
            .through(Serializers.enrichedJsonSerializer(config.compress))
            .through(sinkFor("transformed", idx, config.withEnrichedJson))
          in: Stream[F, GenOutput] => in.broadcastThrough(pipe1, pipe2, pipe3)
        }
      }

    eventStream
      .take(config.payloadsTotal.toLong)
      .zipWithScan1((0, 0)) { case (idx, el) => (el._2.length + idx._1, 1 + idx._2) }
      .evalTap {
        case (_, idx) =>
          if (idx._1 % 10000 == 0 && idx._1 != 0) {
            Sync[F].delay(println(s"processed events: ${idx._1}..."))
          } else if (idx._2 == config.payloadsTotal.toLong) {
            Sync[F].delay(println(
              s"""Payloads = ${idx._2}
                 |Events = ${idx._1}""".stripMargin))
          }
          else {
            Sync[F].unit
          }
      }
      .map(_._1)
      .through(makeOutput)
      .compile
      .drain
  }

}

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

import fs2.{Pipe, Stream}
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
import com.permutive.pubsub.producer.grpc.{GooglePubsubProducer, PubsubProducerConfig}
import com.permutive.pubsub.producer.Model.{ProjectId, Topic}
import com.permutive.pubsub.producer.encoder.MessageEncoder
import scala.concurrent.duration.DurationInt

import java.net.URI
import java.nio.charset.StandardCharsets

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Config.parse(args) match {
      case Right(Config.Cli(config, outputUri)) =>
        sink[IO](outputUri, config) >>
          IO.println(s"""changeFormGenCount  = ${Context.changeFormGenCount}
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

  def sink[F[_]: Async](outputDir: URI, config: Config): F[Unit] = {
    val rng = config.randomisedSeed match {
      case true  => new scala.util.Random(scala.util.Random.nextInt())
      case false => new scala.util.Random(config.seed)
    }

    val timeF = config.timestamps match {
      case Config.Timestamps.Now         => Clock[F].realTimeInstant.flatTap(t => Sync[F].delay(println(s"time: $t")))
      case Config.Timestamps.Fixed(time) => Async[F].pure(time).flatTap(t => Sync[F].delay(println(s"time: $t")))
    }

    val eventStream: Stream[F, GenOutput] =
      Stream.eval(timeF).flatMap { time =>
        config.duplicates match {
          case Some(dups) =>
            Stream.repeatEval(
              Sync[F].delay(
                runGen(
                  SdkEvent.genPairDup(
                    dups.natProb,
                    dups.synProb,
                    dups.natTotal,
                    dups.synTotal,
                    config.eventPerPayloadMin,
                    config.eventPerPayloadMax,
                    time,
                    config.eventFrequencies
                  ),
                  rng
                )
              )
            )
          case None =>
            Stream.repeatEval(
              Sync[F].delay(
                runGen(
                  SdkEvent.genPair(config.eventPerPayloadMin, config.eventPerPayloadMax, time, config.eventFrequencies),
                  rng
                )
              )
            )
        }
      }

    def sinkBuilder(prefix: String, idx: Int): Pipe[F, Byte, Nothing] = {
      val suffix = if (config.compress) ".gz" else ""
      if (outputDir.toString.startsWith("file:")) {
        RotatingSink.file(prefix, suffix, idx, outputDir)
      } else if (outputDir.toString.startsWith("s3:")) {
        RotatingSink.s3(prefix, suffix, idx, outputDir)
      } else {
        throw new RuntimeException(s"Unknown scheme in $outputDir")
      }
    }

    def sinkFor(name: String, idx: Int, predicate: Boolean): Pipe[F, Byte, Nothing] =
      if (predicate) sinkBuilder(name, idx)
      else _.drain

    def fileSink: Pipe[F, GenOutput, Unit] =
      RotatingSink.rotate(config.payloadsPerFile) { idx =>
        val pipe1: Pipe[F, GenOutput, Nothing] =
          _.map(_._1).through(Serializers.rawSerializer(config.compress)).through(sinkFor("raw", idx, config.withRaw))
        val pipe2: Pipe[F, GenOutput, Nothing] = _.flatMap(in => Stream.emits(in._2))
          .through(Serializers.enrichedTsvSerializer(config.compress))
          .through(sinkFor("enriched", idx, config.withEnrichedTsv))
        val pipe3: Pipe[F, GenOutput, Nothing] = _.flatMap(in => Stream.emits(in._2))
          .through(Serializers.enrichedJsonSerializer(config.compress))
          .through(sinkFor("transformed", idx, config.withEnrichedJson))
        in: Stream[F, GenOutput] => in.broadcastThrough(pipe1, pipe2, pipe3)
      }

    def kinesisSink: Pipe[F, GenOutput, Unit] = {
      if (List(config.withRaw, config.withEnrichedTsv, config.withEnrichedJson).count(identity) > 1)
        throw new RuntimeException(s"Kinesis could only output in single format")
      if (config.compress) {
        throw new RuntimeException(s"Kinesis doesn't support compression")
      }
      val kinesisClient: KinesisAsyncClient = KinesisClientUtil.createKinesisAsyncClient(KinesisAsyncClient.builder())

      def reqBuilder(event: Event): PutRecordRequest = PutRecordRequest
        .builder()
        .streamName(outputDir.getRawAuthority)
        .partitionKey(event.event_id.toString)
        .data(SdkBytes.fromUtf8String(event.toTsv))
        .build()

      st: Stream[F, GenOutput] =>
        st.map(_._2)
          .flatMap(Stream.emits)
          .map(reqBuilder)
          .parEvalMap(10)(e => Async[F].fromCompletableFuture(Sync[F].delay(kinesisClient.putRecord(e))))
          .void
    }

    def pubsubSink: Pipe[F, GenOutput, Unit] = {
      if (List(config.withRaw, config.withEnrichedTsv, config.withEnrichedJson).count(identity) > 1)
        throw new RuntimeException(s"Pubsub could only output in single format")
      if (config.compress) {
        throw new RuntimeException(s"Pubsub doesn't support compression")
      }
      val producerConfig = PubsubProducerConfig[F](
        batchSize = 100,
        delayThreshold = 1.second,
        onFailedTerminate = _ => Sync[F].unit
      )
      val topicRegex = "^/*projects/([^/]+)/topics/([^/]+)$".r
      val (projectId, topic) = outputDir.getSchemeSpecificPart match {
        case topicRegex(p, t) => (ProjectId(p), Topic(t))
        case _ =>
          throw new RuntimeException(s"pubsub uri does not match format pubsub://projects/project-id/topics/topic-id")
      }
      implicit val encoder: MessageEncoder[Event] = new MessageEncoder[Event] {
        def encode(e: Event): Either[Throwable, Array[Byte]] =
          e.toTsv.getBytes(StandardCharsets.UTF_8).asRight
      }
      st: Stream[F, GenOutput] =>
        for {
          producer <- Stream.resource(GooglePubsubProducer.of[F, Event](projectId, topic, producerConfig))
          _        <- st.parEvalMapUnordered(1000)(e => e._2.traverse_(producer.produce(_)))
        } yield ()
    }

    def makeOutput: Pipe[F, GenOutput, Unit] =
      outputDir.getScheme match {
        case "kinesis" => kinesisSink
        case "pubsub"  => pubsubSink
        case _         => fileSink
      }

    eventStream
      .take(config.payloadsTotal.toLong)
      .zipWithScan1((0, 0)) { case (idx, el) => (el._2.length + idx._1, 1 + idx._2) }
      .evalTap { case (_, idx) =>
        if (idx._2 == config.payloadsTotal.toLong) {
          Sync[F].delay(println(s"""Payloads = ${idx._2}
                                   |Events = ${idx._1}""".stripMargin))
        } else if (idx._1 % 10000 == 0 && idx._1 != 0) {
          Sync[F].delay(println(s"processed events: ${idx._1}..."))
        } else {
          Sync[F].unit
        }
      }
      .map(_._1)
      .through(makeOutput)
      .compile
      .drain
  }

}

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
package com.snowplowanalytics.snowplow.eventgen.sinks

import java.nio.charset.StandardCharsets
import java.util.UUID

import fs2.{Pipe, Stream}
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerSettings, Serializer}

import cats.syntax.all._

import cats.effect.kernel.{Async, Sync}

import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest
import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload
import com.snowplowanalytics.snowplow.eventgen.Config

object Kafka {

  def make[F[_]: Async](config: Config.Output.Kafka) = new Sink[F] {

    override def collectorPayload: Pipe[F, CollectorPayload, Unit] =
      pipe(
        config,
        collectorPayloadProducer,
        toKafkaRecord[CollectorPayload]
      )

    override def enriched: Pipe[F, String, Unit] =
      pipe(
        config,
        enrichedProducer,
        toKafkaRecord[String]
      )

    override def http: Pipe[F, HttpRequest, Unit] =
      _ => Stream.raiseError(new IllegalStateException(s"Can't use Kafka output for HTTP requests"))
  }

  private def collectorPayloadProducer[F[_]: Sync] = {
    implicit val serializer = Serializer.lift[F, CollectorPayload](cp => Sync[F].pure(cp.toRaw))
    ProducerSettings[F, String, CollectorPayload]
  }

  private def enrichedProducer[F[_]: Sync] = {
    implicit val serializer = Serializer.lift[F, String](e => Sync[F].pure(e.getBytes(StandardCharsets.UTF_8)))
    ProducerSettings[F, String, String]
  }

  private def toKafkaRecord[A](topicName: String, a: A): ProducerRecord[String, A] =
    ProducerRecord(topicName, UUID.randomUUID().toString, a)

  private def pipe[F[_]: Async, A](
    config: Config.Output.Kafka,
    producer: ProducerSettings[F, String, A],
    toRecord: (String, A) => ProducerRecord[String, A]
  ): Pipe[F, A, Unit] =
    _.map(toRecord(config.topic, _))
      .chunkN(100, allowFewer = true)
      .through(
        KafkaProducer.pipe(
          producer
            .withBootstrapServers(config.brokers)
            .withProperties(
              ("key.serializer", "org.apache.kafka.common.serialization.StringSerializer"),
              ("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
            )
            .withProperties(config.producerConf)
        )
      )
      .void
}

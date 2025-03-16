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

import scala.concurrent.duration.DurationInt

import com.permutive.pubsub.producer.grpc.{GooglePubsubProducer, PubsubProducerConfig}
import com.permutive.pubsub.producer.Model.{ProjectId, Topic}
import com.permutive.pubsub.producer.encoder.MessageEncoder

import fs2.{Pipe, Stream}

import cats.syntax.all._

import cats.effect.kernel.{Async, Sync}

import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest
import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload
import com.snowplowanalytics.snowplow.eventgen.Config

object Pubsub {

  def make[F[_]: Async](config: Config.Output.PubSub) = new Sink[F] {

    override def collectorPayload: Pipe[F, CollectorPayload, Unit] = {
      implicit val encoder: MessageEncoder[CollectorPayload] = new MessageEncoder[CollectorPayload] {
        def encode(cp: CollectorPayload): Either[Throwable, Array[Byte]] =
          cp.toRaw.asRight
      }

      pipe(config)
    }

    override def enriched: Pipe[F, String, Unit] = {
      implicit val encoder: MessageEncoder[String] = new MessageEncoder[String] {
        def encode(enriched: String): Either[Throwable, Array[Byte]] =
          enriched.getBytes(StandardCharsets.UTF_8).asRight
      }

      pipe(config)
    }

    override def http: Pipe[F, HttpRequest, Unit] =
      _ => Stream.raiseError(new IllegalStateException(s"Can't use Pubsub output for HTTP requests"))
  }

  private def pipe[F[_]: Async, A: MessageEncoder](config: Config.Output.PubSub): Pipe[F, A, Unit] = {
    val producerConfig = PubsubProducerConfig[F](
      batchSize = 100,
      delayThreshold = 1.second,
      onFailedTerminate = _ => Sync[F].unit
    )
    val topicRegex = "^/*projects/([^/]+)/topics/([^/]+)$".r

    in =>
      for {
        (projectId, topic) <- config.topic match {
          case topicRegex(p, t) => Stream.emit((ProjectId(p), Topic(t)))
          case _ =>
            Stream.raiseError(
              new IllegalArgumentException(s"Pubsub URI does not match format /projects/<project-id>/topics/<topic-id>")
            )
        }
        producer <- Stream.resource(GooglePubsubProducer.of[F, A](projectId, topic, producerConfig))
        _        <- in.parEvalMapUnordered(100)(producer.produce(_))
      } yield ()
  }
}

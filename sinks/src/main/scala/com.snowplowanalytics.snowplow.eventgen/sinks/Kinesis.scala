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

import java.util.UUID

import scala.concurrent.duration._

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.{PutRecordRequest, PutRecordResponse}
import software.amazon.awssdk.regions.Region

import fs2.{Pipe, Stream}

import cats.syntax.all._

import cats.effect.kernel.{Async, Resource, Sync}

import retry.{RetryDetails, RetryPolicies}

import com.snowplowanalytics.snowplow.analytics.scalasdk.Event

import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest
import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload
import com.snowplowanalytics.snowplow.eventgen.Config

object Kinesis {

  def make[F[_]: Async](config: Config.Output.Kinesis) = new Sink[F] {

    override def collectorPayload: Pipe[F, CollectorPayload, Unit] =
      pipe(
        config.region,
        cp =>
          toKinesisRecord[CollectorPayload](
            config.streamName,
            cp,
            _ => UUID.randomUUID().toString,
            cp => SdkBytes.fromByteArray(cp.toRaw)
          )
      )

    override def enriched: Pipe[F, Event, Unit] =
      pipe(
        config.region,
        e => toKinesisRecord[Event](config.streamName, e, _.event_id.toString, e => SdkBytes.fromUtf8String(e.toTsv))
      )

    override def http: Pipe[F, HttpRequest, Unit] =
      _ => Stream.raiseError(new IllegalStateException(s"Can't use Kinesis output for HTTP requests"))
  }

  private def toKinesisRecord[A](
    streamName: String,
    a: A,
    getPartitionKey: A => String,
    getBytes: A => SdkBytes
  ): PutRecordRequest =
    PutRecordRequest.builder().streamName(streamName).partitionKey(getPartitionKey(a)).data(getBytes(a)).build()

  private def pipe[F[_]: Async, A](
    maybeRegion: Option[String],
    mkRecord: A => PutRecordRequest
  ): Pipe[F, A, Unit] = in =>
    for {
      kinesisClient <- Stream.resource(Resource.fromAutoCloseable(Sync[F].delay(maybeRegion match {
        case Some(region) =>
          KinesisAsyncClient.builder.region(Region.of(region)).build
        case None =>
          KinesisAsyncClient.builder.build
      })))
      _ <- in.map(mkRecord).parEvalMap(100)(putRecord(kinesisClient, _))
    } yield ()

  private def putRecord[F[_]: Async](client: KinesisAsyncClient, record: PutRecordRequest): F[PutRecordResponse] = {
    val throttlingPolicy = RetryPolicies.capDelay[F](1.second, RetryPolicies.fibonacciBackoff[F](100.millis))
    def shouldRetry(throwable: Throwable) = Sync[F].delay {
      throwable.isInstanceOf[software.amazon.awssdk.services.kinesis.model.ProvisionedThroughputExceededException]
    }
    def log: (Throwable, RetryDetails) => F[Unit] = (_, _) => Sync[F].delay(println("throttling"))

    val send = Async[F].fromCompletableFuture(Sync[F].delay(client.putRecord(record)))

    retry.retryingOnSomeErrors(throttlingPolicy, shouldRetry, log)(send)
  }
}

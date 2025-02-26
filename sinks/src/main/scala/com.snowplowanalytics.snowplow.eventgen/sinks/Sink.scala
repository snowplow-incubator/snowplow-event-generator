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

import cats.effect.Async

import fs2.Pipe

import com.snowplowanalytics.snowplow.analytics.scalasdk.Event

import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest
import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload
import com.snowplowanalytics.snowplow.eventgen.Config

trait Sink[F[_]] {
  def collectorPayload: Pipe[F, CollectorPayload, Unit]
  def enriched: Pipe[F, Event, Unit]
  def http: Pipe[F, HttpRequest, Unit]
}

object Sink {
  def make[F[_]: Async](config: Config, sinkConfig: Config.Output): Sink[F] = sinkConfig match {
    case fileConfig: Config.Output.File =>
      File.make(config, fileConfig)
    case httpConfig: Config.Output.Http =>
      Http.make(httpConfig)
    case kafkaConfig: Config.Output.Kafka =>
      Kafka.make(kafkaConfig)
    case kinesisConfig: Config.Output.Kinesis =>
      Kinesis.make(kinesisConfig)
    case pubsubConfig: Config.Output.PubSub =>
      Pubsub.make(pubsubConfig)
    case _: Config.Output.Stdout =>
      Stdout.make(config)
  }
}

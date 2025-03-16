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

import java.util.Base64

import fs2.Pipe

import cats.effect.kernel.Sync

import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest
import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload

object Stdout {

  def make[F[_]: Sync] = new Sink[F] {
    private val base64Encoder = Base64.getUrlEncoder

    override def collectorPayload: Pipe[F, CollectorPayload, Unit] =
      pipe(cp => new String(base64Encoder.encode(cp.toRaw)))

    override def enriched: Pipe[F, String, Unit] =
      pipe(identity)

    override def http: Pipe[F, HttpRequest, Unit] =
      pipe(_.toString)
  }

  private def pipe[F[_]: Sync, A](
    stringSerializer: A => String
  ): Pipe[F, A, Unit] =
    _.map(stringSerializer).evalMap(line => Sync[F].delay(println(line)))
}

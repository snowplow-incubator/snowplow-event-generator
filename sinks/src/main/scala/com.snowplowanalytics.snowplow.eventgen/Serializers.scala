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

import cats.effect.Sync
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import fs2.{Pipe, Stream}
import fs2.compression.Compression
import fs2.text.{base64, utf8}

object Serializers {

  def stringSerializer[F[_]: Sync](compress: Boolean): Pipe[F, String, Byte] = {
    val pipe: Pipe[F, String, Byte] = _.flatMap(x => Stream(x, "\n")).through(utf8.encode)
    if (compress)
      pipe.andThen(Compression[F].gzip())
    else
      pipe
  }

  def rawSerializer[F[_] : Sync](compress: Boolean = true): Pipe[F, collector.CollectorPayload, Byte] =
    _.flatMap(e => Stream.emits(e.toRaw))
      .through(base64.encode)
      .through(stringSerializer(compress))

  def enrichedTsvSerializer[F[_] : Sync](compress: Boolean = true): Pipe[F, Event, Byte] =
    _.map(_.toTsv).through(stringSerializer(compress))

  def enrichedJsonSerializer[F[_] : Sync](compress: Boolean = true): Pipe[F, Event, Byte] =
    _.map(_.toJson(true).noSpaces).through(stringSerializer(compress))
}

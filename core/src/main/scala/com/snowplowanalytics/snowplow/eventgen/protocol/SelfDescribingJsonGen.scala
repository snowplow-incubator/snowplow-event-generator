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
package com.snowplowanalytics.snowplow.eventgen.protocol

import com.snowplowanalytics.iglu.core.{SchemaKey, SelfDescribingData}
import com.snowplowanalytics.snowplow.eventgen.protocol.implicits._
import org.scalacheck.Gen
import io.circe.Json
import java.util.concurrent.atomic.AtomicLong
import java.time.Instant

trait SelfDescribingJsonGen {

  def schemaKey: SchemaKey

  def fieldGens(now: Instant): Map[String, Gen[Option[Json]]]

  final val genCount = new AtomicLong()

  final def gen(now: Instant): Gen[SelfDescribingData[Json]] =
    fieldGens(now)
      .genObject
      .map { obj =>
        SelfDescribingData(schemaKey, obj)
      }
      .flatMap { result =>
        Gen.delay(Gen.const(genCount.incrementAndGet())).map(_ => result)
      }
}

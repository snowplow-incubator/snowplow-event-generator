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
import io.circe.{Json, JsonObject}
import io.circe.syntax._
import java.util.concurrent.atomic.AtomicLong
import java.time.Instant

trait SelfDescribingJsonGen {

  def schemaKey: SchemaKey

  def fieldGens(now: Instant): Map[String, Gen[Option[Json]]]

  val allowsAdditionalProperties: Boolean = false

  final val genCount = new AtomicLong()

  final def gen(now: Instant): Gen[SelfDescribingData[Json]] = {
    val fields = fieldGens(now)
    fields
      .genObject
      .flatMap { obj =>
        if (allowsAdditionalProperties)
          SelfDescribingJsonGen.genAdditionalProperties.map { extras =>
            val definedKeys = fields.keySet
            val filtered    = extras.filterNot { case (k, _) => definedKeys.contains(k) }
            obj.asObject match {
              case Some(o) => JsonObject.fromIterable(o.toList ++ filtered).asJson
              case None    => obj
            }
          }
        else
          Gen.const(obj)
      }
      .map { obj =>
        SelfDescribingData(schemaKey, obj)
      }
      .flatMap { result =>
        Gen.delay(Gen.const(genCount.incrementAndGet())).map(_ => result)
      }
  }
}

object SelfDescribingJsonGen {

  private val genKey: Gen[String] =
    Gen.chooseNum(1, 10).flatMap(Gen.stringOfN(_, Gen.alphaLowerChar))

  private val genField: Gen[(String, Json)] =
    for {
      key   <- genKey
      value <- Gen.chooseNum(1, 20).flatMap(Gen.stringOfN(_, Gen.alphaNumChar))
    } yield (key, Json.fromString(value))

  val genAdditionalProperties: Gen[List[(String, Json)]] =
    Gen.chooseNum(1, 5).flatMap(Gen.listOfN(_, genField))
}

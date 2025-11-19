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
package com.snowplowanalytics.snowplow.eventgen.protocol.contexts

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer, SelfDescribingData}
import com.snowplowanalytics.snowplow.eventgen.protocol.SelfDescribingJsonGen
import com.snowplowanalytics.snowplow.eventgen.protocol.implicits._
import org.scalacheck.Gen
import io.circe.Json
import java.time.Instant

/** Ecommerce user context containing user id for identity correlation. Uses the standard ecommerce user schema from
  * Iglu Central.
  */
object AuthenticatedUser extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey(
      "com.snowplowanalytics.snowplow.ecommerce",
      "user",
      "jsonschema",
      SchemaVer.Full(1, 0, 0)
    )

  def genWithUserId(userId: Option[String]): Gen[Option[SelfDescribingData[Json]]] =
    userId match {
      case Some(id) =>
        val fields = Map(
          "id"       -> Gen.const(id).required,
          "is_guest" -> Gen.const(false).optional,
          "email"    -> Gen.const(s"$id@example.com").optional
        )
        fields.genObject.map { obj =>
          Some(SelfDescribingData(schemaKey, obj))
        }
      case None =>
        Gen.const(None)
    }

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "id"       -> Gen.uuid.map(_.toString).required,
      "is_guest" -> Gen.oneOf(true, false).optional,
      "email"    -> Gen.const("user@example.com").optional
    )
}

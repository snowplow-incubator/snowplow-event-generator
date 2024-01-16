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
package com.snowplowanalytics.snowplow.eventgen.protocol.contexts

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer}
import com.snowplowanalytics.snowplow.eventgen.protocol.SelfDescribingJsonGen
import com.snowplowanalytics.snowplow.eventgen.protocol.implicits._
import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.scalacheck.Gen
import io.circe.Json
import java.time.Instant

object UserData extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.google.tag-manager.server-side", "user_data", "jsonschema", SchemaVer.Full(1, 0, 0))

  val addressGen: Gen[Json] =
    Map(
      "first_name"  -> strGen(1, 200).optionalOrNull,
      "last_name"   -> strGen(1, 200).optionalOrNull,
      "street"      -> strGen(1, 200).optionalOrNull,
      "city"        -> strGen(1, 200).optionalOrNull,
      "region"      -> strGen(1, 200).optionalOrNull,
      "postal_code" -> strGen(1, 200).optionalOrNull,
      "country"     -> strGen(1, 200).optionalOrNull
    ).genObject

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "email_address" -> strGen(1, 256).optionalOrNull,
      "phone_number"  -> strGen(1, 64).optionalOrNull,
      "address"       -> addressGen.optionalOrNull
    )
}

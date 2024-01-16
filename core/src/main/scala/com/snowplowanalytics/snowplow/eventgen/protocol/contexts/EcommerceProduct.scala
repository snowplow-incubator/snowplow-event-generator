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

object EcommerceProduct extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow.ecommerce", "product", "jsonschema", SchemaVer.Full(1, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "id"               -> strGen(1, 200).required,
      "name"             -> strGen(1, 200).optionalOrNull,
      "category"         -> strGen(1, 200).required,
      "price"            -> Gen.chooseNum(0L, 9999999L).map(unscaled => BigDecimal(unscaled, 2)).required,
      "list_price"       -> Gen.chooseNum(0L, 9999999L).map(unscaled => BigDecimal(unscaled, 2)).optionalOrNull,
      "quantity"         -> Gen.chooseNum(0, 9999999).optionalOrNull,
      "size"             -> strGen(1, 200).optionalOrNull,
      "variant"          -> strGen(1, 200).optionalOrNull,
      "brand"            -> strGen(1, 200).optionalOrNull,
      "inventory_status" -> strGen(1, 200).optionalOrNull,
      "position"         -> Gen.chooseNum(0, 9999999).optionalOrNull,
      "currency"         -> strGen(3, 3).required
    )
}

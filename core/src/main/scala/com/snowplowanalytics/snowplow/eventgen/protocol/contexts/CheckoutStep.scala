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

object CheckoutStep extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow.ecommerce", "checkout_step", "jsonschema", SchemaVer.Full(1, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "step"                  -> Gen.chooseNum(1, 90).required,
      "shipping_postcode"     -> strGen(1, 128).optionalOrNull,
      "billing_postcode"      -> strGen(1, 128).optionalOrNull,
      "shipping_full_address" -> strGen(1, 200).optionalOrNull,
      "billing_full_address"  -> strGen(1, 200).optionalOrNull,
      "delivery_provider"     -> strGen(1, 128).optionalOrNull,
      "delivery_method"       -> strGen(1, 128).optionalOrNull,
      "coupon_code"           -> strGen(1, 128).optionalOrNull,
      "account_type"          -> strGen(1, 128).optionalOrNull,
      "payment_method"        -> strGen(1, 128).optionalOrNull,
      "proof_of_payment"      -> strGen(1, 128).optionalOrNull,
      "marketing_opt_in"      -> genBool.optionalOrNull
    )
}

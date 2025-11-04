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
import scala.math.BigInt
import java.time.Instant

object MobileContext extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow", "mobile_context", "jsonschema", SchemaVer.Full(1, 0, 3))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "osType"                -> strGen(1, 20).required,
      "osVersion"             -> strGen(1, 20).required,
      "deviceManufacturer"    -> strGen(1, 20).required,
      "deviceModel"           -> strGen(1, 20).required,
      "carrier"               -> strGen(1, 20).optionalOrNull,
      "networkType"           -> Gen.oneOf("mobile", "wifi", "offline").optionalOrNull,
      "networkTechnology"     -> strGen(1, 20).optionalOrNull,
      "openIdfa"              -> strGen(1, 20).optionalOrNull,
      "appleIdfa"             -> strGen(1, 20).optionalOrNull,
      "androidIdfa"           -> strGen(1, 20).optionalOrNull,
      "systemAvailableMemory" -> Gen.chooseNum(BigInt(0), BigInt("9223372036854775000")).optionalOrNull,
      "appAvailableMemory"    -> Gen.chooseNum(BigInt(0), BigInt("9223372036854775000")).optionalOrNull,
      "batteryLevel"          -> Gen.chooseNum(0, 100).optionalOrNull,
      "batteryState"          -> Gen.oneOf("unplugged", "charging", "full").optionalOrNull,
      "lowPowerMode"          -> genBool.optionalOrNull,
      "availableStorage"      -> Gen.chooseNum(BigInt(0), BigInt("9223372036854775000")).optionalOrNull,
      "totalStorage"          -> Gen.chooseNum(BigInt(0), BigInt("9223372036854775000")).optionalOrNull,
      "isPortrait"            -> genBool.optionalOrNull,
      "resolution"            -> strGen(1, 20).optionalOrNull,
      "scale"                 -> Gen.chooseNum(0, 100.1).required,
      "language"              -> strGen(1, 8).optionalOrNull,
      "appSetId"              -> Gen.uuid.optionalOrNull,
      "appSetIdScope"         -> Gen.oneOf("app", "developer").optionalOrNull
    )
}

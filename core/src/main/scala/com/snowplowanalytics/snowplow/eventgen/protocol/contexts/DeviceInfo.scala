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

object DeviceInfo extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.roku", "device_info", "jsonschema", SchemaVer.Full(1, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "model"                 -> strGen(1, 255).required,
      "modelDisplayName"      -> strGen(1, 255).required,
      "modelType"             -> strGen(1, 255).required,
      "osVersion"             -> strGen(1, 255).required,
      "channelClientId"       -> strGen(1, 255).optionalOrNull,
      "isRIDADisabled"        -> genBool.optionalOrNull,
      "RIDA"                  -> strGen(1, 255).optionalOrNull,
      "captionsMode"          -> strGen(1, 255).required,
      "audioOutputChannel"    -> strGen(1, 255).required,
      "memoryLevel"           -> strGen(1, 255).orNull,
      "timeSinceLastKeypress" -> Gen.chooseNum(0, 2147483647).required,
      "userCountryCode"       -> strGen(1, 255).orNull,
      "countryCode"           -> strGen(1, 255).required,
      "videoMode"             -> strGen(1, 255).required,
      "displayWidth"          -> Gen.chooseNum(0, 65535).required,
      "displayHeight"         -> Gen.chooseNum(0, 65535).required,
      "displayProperties"     -> Gen.chooseNum(0, 5).flatMap(n => Gen.listOfN(n, strGen(1, 255))).required,
      "connectionType"        -> strGen(1, 255).required,
      "internetStatus"        -> genBool.optionalOrNull,
      "features"              -> Gen.chooseNum(0, 5).flatMap(n => Gen.listOfN(n, strGen(1, 255))).required
    )
}

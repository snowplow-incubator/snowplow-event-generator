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

object GeolocationContext extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow", "geolocation_context", "jsonschema", SchemaVer.Full(1, 1, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "latitude"                  -> Gen.chooseNum(-90.0, 90.0).required,
      "longitude"                 -> Gen.chooseNum(-180.0, 180.0).required,
      "latitudeLongitudeAccuracy" -> Gen.chooseNum(0.0, 10.0).optionalOrNull,
      "altitude"                  -> Gen.chooseNum(0.0, 100000.0).optionalOrNull,
      "altitudeAccuracy"          -> Gen.chooseNum(0.0, 10.0).optionalOrNull,
      "bearing"                   -> Gen.chooseNum(0.0, 360.0).optionalOrNull,
      "speed"                     -> Gen.chooseNum(0.0, 1000.0).optionalOrNull,
      "timestamp"                 -> genInstant(now).map(_.toEpochMilli).optionalOrNull
    )
}

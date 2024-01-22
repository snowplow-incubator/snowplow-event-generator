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
package com.snowplowanalytics.snowplow.eventgen.protocol.unstructs

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer}
import com.snowplowanalytics.snowplow.eventgen.protocol.SelfDescribingJsonGen
import com.snowplowanalytics.snowplow.eventgen.protocol.implicits._
import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.scalacheck.Gen
import io.circe.Json
import java.time.Instant

object QualityChangeEvent extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow.media", "quality_change_event", "jsonschema", SchemaVer.Full(1, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "previousQuality" -> strGen(1, 100).optionalOrNull,
      "newQuality"      -> strGen(1, 100).optionalOrNull,
      "bitrate"         -> Gen.chooseNum(0L, 9007199254740991L).optionalOrNull,
      "framesPerSecond" -> Gen.chooseNum(0, 65535).optionalOrNull,
      "automatic"       -> genBool.optionalOrNull
    )

}

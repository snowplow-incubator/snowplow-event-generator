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

object RokuVideo extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.roku", "video", "jsonschema", SchemaVer.Full(1, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "videoId"              -> strGen(1, 255).required,
      "contentId"            -> strGen(1, 255).optionalOrNull,
      "contentTitle"         -> strGen(1, 255).optionalOrNull,
      "contentUrl"           -> Url.gen.map(_.toString).optionalOrNull,
      "contentType"          -> strGen(1, 255).optionalOrNull,
      "streamFormat"         -> strGen(1, 255).optionalOrNull,
      "streamUrl"            -> Url.gen.map(_.toString).optionalOrNull,
      "measuredBitrate"      -> Gen.choose(0, 2147483647).optionalOrNull,
      "streamBitrate"        -> Gen.choose(0, 2147483647).optionalOrNull,
      "isUnderrun"           -> genBool.optionalOrNull,
      "isResumed"            -> genBool.optionalOrNull,
      "videoFormat"          -> strGen(1, 255).optionalOrNull,
      "timeToStartStreaming" -> Gen.chooseNum(0L, 9007199254740991L).optionalOrNull,
      "width"                -> Gen.choose(0, 65535).required,
      "height"               -> Gen.choose(0, 65535).required,
      "errorStr"             -> strGen(1, 255).optionalOrNull
    )
}

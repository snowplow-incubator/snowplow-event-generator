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

object BrowserContext extends SelfDescribingJsonGen {

  override val schemaKey: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow", "browser_context", "jsonschema", SchemaVer.Full(2, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "viewport"            -> strGen(1, 20).required,
      "documentSize"        -> strGen(1, 20).required,
      "resolution"          -> strGen(1, 20).required,
      "colorDepth"          -> Gen.chooseNum(0, 1000).required,
      "devicePixelRatio"    -> Gen.chooseNum(0, 1000).optionalOrNull,
      "cookiesEnabled"      -> genBool.required,
      "online"              -> genBool.required,
      "browserLanguage"     -> strGen(1, 20).optionalOrNull,
      "documentLanguage"    -> strGen(1, 20).optionalOrNull,
      "webdriver"           -> genBool.optionalOrNull,
      "deviceMemory"        -> Gen.chooseNum(0, 1000).optionalOrNull,
      "hardwareConcurrency" -> Gen.chooseNum(0, 1000).optionalOrNull,
      "tabId"               -> Gen.uuid.optionalOrNull
    )
}

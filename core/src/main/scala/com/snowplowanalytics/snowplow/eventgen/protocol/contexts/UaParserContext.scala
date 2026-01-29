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

object UaParserContext extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow", "ua_parser_context", "jsonschema", SchemaVer.Full(1, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "useragentFamily"  -> Gen.oneOf("Chrome", "Firefox", "Safari").required,
      "useragentMajor"   -> strGen(1, 32).required,
      "useragentMinor"   -> strGen(1, 32).required,
      "useragentPatch"   -> strGen(1, 32).required,
      "useragentVersion" -> strGen(1, 128).required,
      "osFamily"         -> Gen.oneOf("Linux", "Windows", "Mac OS X").required,
      "osMajor"          -> strGen(1, 32).optional,
      "osMinor"          -> strGen(1, 32).optional,
      "osPatch"          -> strGen(1, 32).optional,
      "osPatchMinor"     -> strGen(1, 32).optional,
      "osVersion"        -> strGen(1, 128).optional,
      "deviceFamily"     -> Gen.oneOf("Mac", "iPhone", "Generic Feature Phone").required
    )
}

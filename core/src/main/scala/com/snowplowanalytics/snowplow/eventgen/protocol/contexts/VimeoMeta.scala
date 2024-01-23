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

object VimeoMeta extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.vimeo", "meta", "jsonschema", SchemaVer.Full(1, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "videoId"     -> Gen.chooseNum(BigInt(0), BigInt("9223372036854776000")).required,
      "videoTitle"  -> strGen(1, 200).required,
      "videoUrl"    -> Url.gen.map(_.toString).optionalOrNull,
      "videoWidth"  -> Gen.chooseNum(BigInt(0), BigInt("9223372036854776000")).required,
      "videoHeight" -> Gen.chooseNum(BigInt(0), BigInt("9223372036854776000")).required
    )
}

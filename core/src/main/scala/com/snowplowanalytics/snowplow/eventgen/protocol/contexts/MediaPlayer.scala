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

object MediaPlayer extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow", "media_player", "jsonschema", SchemaVer.Full(2, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "currentTime"      -> Gen.chooseNum(0, 2147483647).required,
      "ended"            -> genBool.required,
      "fullscreen"       -> genBool.optionalOrNull,
      "livestream"       -> genBool.optionalOrNull,
      "label"            -> strGen(1, 200).optionalOrNull,
      "loop"             -> genBool.optionalOrNull,
      "mediaType"        -> Gen.oneOf("video", "audio").optionalOrNull,
      "muted"            -> genBool.optionalOrNull,
      "paused"           -> genBool.required,
      "pictureInPicture" -> genBool.optionalOrNull,
      "playbackRate"     -> Gen.chooseNum(0, 16).optionalOrNull,
      "playerType"       -> strGen(1, 200).optionalOrNull,
      "quality"          -> strGen(1, 200).optionalOrNull,
      "volume"           -> Gen.chooseNum(0, 100).optionalOrNull
    )
}

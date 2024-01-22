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

object Spamreport extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.sendgrid", "spamreport", "jsonschema", SchemaVer.Full(3, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "timestamp"                   -> genInstant(now).map(_.toString).optional,
      "email"                       -> strGen(1, 320).optional,
      "sg_event_id"                 -> strGen(22, 100).optional,
      "smtp-id"                     -> strGen(1, 100).optional,
      "category"                    -> Gen.listOfN(5, strGen(1, 5)).optional,
      "asm_group_id"                -> Gen.chooseNum(BigInt(0), BigInt("9223372036854775807")).optional,
      "sg_message_id"               -> strGen(1, 100).optional,
      "marketing_campaign_id"       -> Gen.chooseNum(1, Int.MaxValue).optional,
      "marketing_campaign_name"     -> strGen(1, 100).optional,
      "marketing_campaign_version"  -> strGen(1, 100).optional,
      "marketing_campaign_split_id" -> Gen.chooseNum(1, Int.MaxValue).optional
    )

}

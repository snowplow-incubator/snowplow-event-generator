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
package com.snowplowanalytics.snowplow.eventgen.tracker

import com.snowplowanalytics.iglu.core.{SchemaKey, SelfDescribingData}
import com.snowplowanalytics.snowplow.eventgen.protocol.Body
import com.snowplowanalytics.snowplow.eventgen.protocol.event.EventFrequencies
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._
import com.snowplowanalytics.snowplow.eventgen.protocol.common.PayloadDataSchema
import io.circe.Json
import io.circe.syntax._
import org.scalacheck.Gen

import java.time.Instant

final case class HttpRequestBody(schema: SchemaKey, data: List[Body]) {
  def toJson: Json = SelfDescribingData(PayloadDataSchema.Default, data.map(_.toPayloadElement).asJson).asJson

  override def toString: String = toJson.noSpaces
}

object HttpRequestBody {
  def genDup(
    natProb: Float,
    synProb: Float,
    natTotal: Int,
    synTotal: Int,
    min: Int,
    max: Int,
    now: Instant,
    frequencies: EventFrequencies
  ): Gen[HttpRequestBody] = genWithBody(min, max, Body.genDup(natProb, synProb, natTotal, synTotal, now, frequencies))

  def gen(
    min: Int, 
    max: Int, 
    now: Instant,     
    frequencies: EventFrequencies): Gen[HttpRequestBody] = genWithBody(min, max, Body.gen(now, frequencies))

  private def genWithBody(min: Int, max: Int, bodyGen: Gen[Body]) =
    for {
      n       <- Gen.chooseNum(min, max)
      payload <- Gen.listOfN(n, bodyGen)
    } yield HttpRequestBody(PayloadDataSchema.Default, payload)
}

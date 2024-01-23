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
package com.snowplowanalytics.snowplow.eventgen.protocol.event

import com.snowplowanalytics.snowplow.eventgen.protocol.unstructs.{ChangeForm, FunnelInteraction, LinkClick}
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent
import com.snowplowanalytics.snowplow.eventgen.primitives.base64Encode
import io.circe.Json
import io.circe.syntax._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen
import java.time.Instant

case class UnstructEventFrequencies(changeForm: Int, funnelInteraction: Int, linkClick: Int)

final case class UnstructEventWrapper(
  event: SelfDescribingData[Json],
  b64: Boolean
) extends BodyEvent {
  override def toProto: List[BasicNameValuePair] = {
    val asJson = SnowplowEvent.UnstructEvent(Some(event)).asJson
    if (!b64)
      asKV("ue_pr", Some(asJson.noSpaces))
    else
      asKV("ue_px", Some(base64Encode(asJson)))
  }
}

object UnstructEventWrapper {
  def gen(now: Instant, frequencies: UnstructEventFrequencies): Gen[UnstructEventWrapper] =
    Gen
      .frequency(
        frequencies.linkClick         -> LinkClick.gen(now),
        frequencies.changeForm        -> ChangeForm.gen(now),
        frequencies.funnelInteraction -> FunnelInteraction.gen(now)
      )
      .map(l => UnstructEventWrapper(l, b64 = true))
}

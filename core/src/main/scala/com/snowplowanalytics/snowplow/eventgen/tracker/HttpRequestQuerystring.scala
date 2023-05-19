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

import com.snowplowanalytics.snowplow.eventgen.protocol.{Body, Protocol}
import com.snowplowanalytics.snowplow.eventgen.protocol.Body.encodeValue
import com.snowplowanalytics.snowplow.eventgen.protocol.event.EventFrequencies
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen

import java.time.Instant

final case class HttpRequestQuerystring(qs: List[BasicNameValuePair]) extends Protocol {
  override def toProto: List[BasicNameValuePair] = qs
  override def toString: String = qs.map(kv => s"${kv.getName}=${encodeValue(kv.getValue)}").mkString("&")
}

object HttpRequestQuerystring {
  def gen(now: Instant, frequencies: EventFrequencies): Gen[HttpRequestQuerystring] = genWithBody(Body.gen(now, frequencies))

  private def genWithBody(bodyGen: Gen[Body]) =
    bodyGen.flatMap(qs =>
      HttpRequestQuerystring(qs.toProto.map(kv => new BasicNameValuePair(kv.getName, encodeValue(kv.getValue))))
    )
}

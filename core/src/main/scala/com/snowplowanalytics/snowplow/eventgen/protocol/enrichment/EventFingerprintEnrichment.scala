/*
 * Copyright (c) 2021-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.eventgen.protocol.enrichment

import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.scalacheck.Gen

case class EventFingerprintEnrichment(
  event_fingerprint: Option[String]
)

object EventFingerprintEnrichment {
  def gen: Gen[EventFingerprintEnrichment] =
    genStringOpt("event_fingerprint", 10).map(EventFingerprintEnrichment.apply)
}

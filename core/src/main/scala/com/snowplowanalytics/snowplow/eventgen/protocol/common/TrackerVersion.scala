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
package com.snowplowanalytics.snowplow.eventgen.protocol.common

import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen

final case class TrackerVersion(
  tv: String // v_tracker
) extends Protocol {
  override def toProto: List[BasicNameValuePair] = asKV("tv", Some(tv))

}

object TrackerVersion {
  def gen: Gen[TrackerVersion] = Gen.oneOf("scala-tracker_1.0.0", "js_2.0.0", "go_1.2.3").map(TrackerVersion.apply)
}

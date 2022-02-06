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

import com.snowplowanalytics.snowplow.eventgen.protocol.common.Web
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen

final case class PageView(
                     override val deps: List[Web] // all web props
                   ) extends LegacyEvent {
  override def toProto: List[BasicNameValuePair] = deps.flatMap(_.toProto)
}

object PageView {
  def gen: Gen[PageView] = Gen.listOfN(1, Web.gen).map(PageView.apply)
}

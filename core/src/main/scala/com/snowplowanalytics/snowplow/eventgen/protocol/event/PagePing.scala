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
import org.scalacheck.Gen
import com.snowplowanalytics.snowplow.eventgen.primitives._
import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.cats.implicits._

final case class PagePing(
                     override val deps: List[Protocol], // all web props
                     pp_mix: Option[Int], // pp_xoffset_min
                     pp_max: Option[Int], //pp_xoffset_max
                     pp_miy: Option[Int], //pp_yoffset_min
                     pp_may: Option[Int] //pp_yoffset_max
                   ) extends LegacyEvent {


  override def toProto: List[BasicNameValuePair] =
    deps.flatMap(_.toProto) ++
      asKV("pp_mix", pp_mix) ++
      asKV("pp_max", pp_max) ++
      asKV("pp_miy", pp_miy) ++
      asKV("pp_may", pp_may)

}

object PagePing {
  def gen: Gen[PagePing] = (
    Gen.listOfN(1, Web.gen),
    genIntOpt,
    genIntOpt,
    genIntOpt,
    genIntOpt
    ).mapN(PagePing.apply)
}

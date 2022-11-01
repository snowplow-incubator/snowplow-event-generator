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

import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._

final case class StructEvent(
  se_ca: Option[String], //	se_category
  se_ac: Option[String], //	se_action
  se_la: Option[String], //	se_label
  se_pr: Option[String], //	se_property
  se_va: Option[Double]  //	se_value
) extends BodyEvent {
  override def toProto: List[BasicNameValuePair] =
    asKV("se_ca", se_ca) ++
      asKV("se_ac", se_ac) ++
      asKV("se_la", se_la) ++
      asKV("se_pr", se_pr) ++
      asKV("se_va", se_va)
}

object StructEvent {
  def gen: Gen[StructEvent] =
    (
      genStringOpt("se_ca", 10),
      genStringOpt("se_ca", 10),
      genStringOpt("se_ca", 10),
      genStringOpt("se_ca", 10),
      genDblOpt
    ).mapN(StructEvent.apply)
}

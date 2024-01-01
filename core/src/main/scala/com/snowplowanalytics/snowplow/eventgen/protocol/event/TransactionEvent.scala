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
package com.snowplowanalytics.snowplow.eventgen.protocol.event

import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._

final case class TransactionEvent(
  tr_id: String,         // tr_orderid
  tr_af: Option[String], // tr_affiliation
  tr_tt: Double,         // tr_total
  tr_tx: Option[Double], // tr_tax
  tr_sh: Option[Double], // tr_shipping
  tr_ci: Option[String], // tr_city
  tr_st: Option[String], // tr_state
  tr_co: Option[String], // tr_country
  tr_cu: Option[String]  // tr_currency
) extends BodyEvent {
  override def toProto: List[BasicNameValuePair] =
    asKV("tr_id", Some(tr_id)) ++
      asKV("tr_af", tr_af) ++
      asKV("tr_tt", Some(tr_tt)) ++
      asKV("tr_tx", tr_tx) ++
      asKV("tr_sh", tr_sh) ++
      asKV("tr_ci", tr_ci) ++
      asKV("tr_st", tr_st) ++
      asKV("tr_co", tr_co) ++
      asKV("tr_cu", tr_cu)
}

object TransactionEvent {
  def gen: Gen[TransactionEvent] =
    (
      genString("tr_orderid", 10),
      genStringOpt("tr_affiliation", 10),
      genScale2Double,
      genScale2DoubleOpt,
      genScale2DoubleOpt,
      genStringOpt("tr_city", 10),
      genStringOpt("tr_state", 10),
      genStringOpt("tr_country", 10),
      genStringOpt("tr_currency", 10)
    ).mapN(TransactionEvent.apply)
}

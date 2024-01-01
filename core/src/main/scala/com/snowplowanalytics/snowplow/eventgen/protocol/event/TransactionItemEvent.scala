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

final case class TransactionItemEvent(
  ti_id: String,         // ti_orderid
  ti_sk: String,         // ti_sku
  ti_nm: Option[String], // ti_name
  ti_ca: Option[String], // ti_category
  ti_pr: Double,         // ti_price
  ti_qu: Int,            // ti_quantity
  ti_cu: Option[String], //ti_currency
) extends BodyEvent {
  override def toProto: List[BasicNameValuePair] =
    asKV("ti_id", Some(ti_id)) ++
      asKV("ti_sk", Some(ti_sk)) ++
      asKV("ti_nm", ti_nm) ++
      asKV("ti_ca", ti_ca) ++
      asKV("ti_pr", Some(ti_pr)) ++
      asKV("ti_qu", Some(ti_qu)) ++
      asKV("ti_cu", ti_cu)
}

object TransactionItemEvent {
  def gen: Gen[TransactionItemEvent] =
    (
      genString("ti_orderid", 10),
      genString("ti_sku", 10),
      genStringOpt("ti_name", 10),
      genStringOpt("ti_category", 10),
      genScale2Double,
      genInt,
      genStringOpt("ti_currency", 10)
    ).mapN(TransactionItemEvent.apply)
}

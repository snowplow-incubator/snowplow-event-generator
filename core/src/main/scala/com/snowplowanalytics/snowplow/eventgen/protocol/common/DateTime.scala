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

import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.scalacheck.Gen
import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.cats.implicits._

import java.time.Instant

final case class DateTime(
                     dtm: Option[Instant], // dvce_created_tstamp
                     stm: Option[Instant], // dvce_sent_tstamp
                     ttm: Option[Instant], // true_tstamp
                     tz: Option[String] // os_timezone
                   ) extends Protocol {
  override def toProto: List[BasicNameValuePair] =
    asKV("dtm", dtm) ++
      asKV("stm", stm) ++
      asKV("ttm", ttm) ++
      asKV("tz", tz)

}

object DateTime {
  def gen(now: Instant): Gen[DateTime] = (genInstantOpt(now), genInstantOpt(now), genInstantOpt(now), genTzOpt).mapN(DateTime.apply)

  def genOpt(now: Instant): Gen[Option[DateTime]] = Gen.option(gen(now))
}

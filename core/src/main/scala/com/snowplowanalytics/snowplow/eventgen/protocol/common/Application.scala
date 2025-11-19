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

import org.scalacheck.Gen
import com.snowplowanalytics.snowplow.eventgen.primitives._
import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.cats.implicits._

final case class Application(
  p: String,           // platform
  aid: Option[String], // app_id
//                        evn: Option[String], // event_vendor deprecated
  tna: Option[String] //  The tracker namespace
) extends Protocol {
  override def toProto: List[BasicNameValuePair] =
    asKV("p", Some(p)) ++
      asKV("aid", aid) ++
//    asKV("evn", evn) ++
      asKV("tna", tna)

}

object Application {
  def gen: Gen[Application] =
    (
      Gen.oneOf("web", "mob", "pc", "srv", "app", "tv", "cnsl", "iot"),
      genStringOpt("aid", 10),
//    genStringOpt("evn", 10),
      genStringOpt("tna", 10)
    ).mapN(Application.apply)

  /** Generate Application with a specific app_id (for multi-profile scenarios).
    */
  def genWithAppId(appId: String): Gen[Application] =
    (
      Gen.oneOf("web", "mob", "pc", "srv", "app", "tv", "cnsl", "iot"),
      Gen.const(Some(appId)),
      genStringOpt("tna", 10)
    ).mapN(Application.apply)
}

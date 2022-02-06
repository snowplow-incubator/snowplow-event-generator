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

import java.util.UUID

final case class User(
                 duid: Option[String], // domain_userid
                 nuid: Option[UUID], // network_userid
                 tnuid: Option[UUID], // network_userid
                 uid: Option[String], // user_id
                 vid: Option[Int], // domain_sessionidx
                 sid: Option[UUID], // domain_sessionid
                 ip: Option[String] // user_ipaddress
               ) extends Protocol {
  override def toProto: List[BasicNameValuePair] =
    asKV("duid", duid) ++
      asKV("nuid", nuid) ++
      asKV("tnuid", tnuid) ++
      asKV("uid", uid) ++
      asKV("vid", vid) ++
      asKV("sid", sid) ++
      asKV("ip", ip)

}

object User {
  def gen: Gen[User] = (
    genStringOpt("duid", 10),
    Gen.option(Gen.uuid),
    Gen.option(Gen.uuid),
    genStringOpt("uid", 10),
    genIntOpt, Gen.option(Gen.uuid),
    genIpOpt).mapN(User.apply)

  def genOpt: Gen[Option[User]] = Gen.option(gen)
}

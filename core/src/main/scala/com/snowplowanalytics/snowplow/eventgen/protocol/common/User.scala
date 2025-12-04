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
import com.snowplowanalytics.snowplow.eventgen.GenConfig
import org.scalacheck.Gen
import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.cats.implicits._

import java.util.UUID

final case class User(
  duid: Option[String], // domain_userid
  nuid: Option[UUID],   // network_userid
  tnuid: Option[UUID],  // network_userid
  uid: Option[String],  // user_id
  vid: Option[Int],     // domain_sessionidx
  sid: Option[UUID],    // domain_sessionid
  ip: Option[String]    // user_ipaddress
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
  def gen: Gen[User] =
    (
      genStringOpt("duid", 10),
      Gen.option(Gen.uuid),
      Gen.option(Gen.uuid),
      genStringOpt("uid", 10),
      genIntOpt,
      Gen.option(Gen.uuid),
      genIpOpt
    ).mapN(User.apply)

  /** Generate User fields from a specific identity using algorithmic approach. No pre-computation required - computes
    * cluster membership on-demand.
    *
    * @param userId
    *   The synthetic identity ID
    * @param config
    *   Identity graph configuration
    * @return
    *   Gen that produces a User with mix of unique and shared identifiers
    */
  def genFromIdentity(userId: Long, config: GenConfig.UserGraph): Gen[User] =
    for {
      cookieIdx  <- Gen.choose(0, config.maxIdentifiers("cookie") - 1)
      deviceIdx  <- Gen.choose(0, config.maxIdentifiers("device") - 1)
      ipIdx      <- Gen.choose(0, config.maxIdentifiers("ip") - 1)
      sessionIdx <- Gen.choose(0, 19)
    } yield {
      val allCookies = ClusterAlgorithm.generateIdentifiers(
        userId,
        "cookie",
        config.maxIdentifiers("cookie"),
        config.numUsers,
        config.sharedIdentifierRate,
        config.usersPerCluster,
        config.sharedIdentifiersForType("cookie")
      )

      val allDevices = ClusterAlgorithm.generateIdentifiers(
        userId,
        "device",
        config.maxIdentifiers("device"),
        config.numUsers,
        config.sharedIdentifierRate,
        config.usersPerCluster,
        config.sharedIdentifiersForType("device")
      )

      val allIPs = ClusterAlgorithm.generateIdentifiers(
        userId,
        "ip",
        config.maxIdentifiers("ip"),
        config.numUsers,
        config.sharedIdentifierRate,
        config.usersPerCluster,
        config.sharedIdentifiersForType("ip")
      )

      User(
        duid = Some(allCookies(cookieIdx)),
        nuid = Some(UUID.fromString(allDevices(deviceIdx))),
        tnuid = Some(UUID.fromString(allDevices(deviceIdx))),
        uid = None,
        vid = Some(sessionIdx),
        sid = {
          val sessionSeed = s"${userId}_session_${sessionIdx}"
          Some(UUID.nameUUIDFromBytes(sessionSeed.getBytes))
        },
        ip = Some(allIPs(ipIdx))
      )
    }

  def genOpt: Gen[Option[User]] = Gen.option(gen)
}

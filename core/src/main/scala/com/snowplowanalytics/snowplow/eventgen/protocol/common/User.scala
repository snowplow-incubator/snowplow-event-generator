package com.snowplowanalytics.snowplow.eventgen.protocol.common

import com.snowplowanalytics.snowplow.eventgen.utils._
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

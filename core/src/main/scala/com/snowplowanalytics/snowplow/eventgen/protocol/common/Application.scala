package com.snowplowanalytics.snowplow.eventgen.protocol.common

import org.scalacheck.Gen
import com.snowplowanalytics.snowplow.eventgen.base._
import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.cats.implicits._

final case class Application(
                        p: String, // platform
                        aid: Option[String], // app_id
//                        evn: Option[String], // event_vendor deprecated
                        tna: Option[String], //  The tracker namespace
                      ) extends Protocol {
  override def toProto: List[BasicNameValuePair] =
    asKV("p", Some(p)) ++
    asKV("aid", aid) ++
//    asKV("evn", evn) ++
    asKV("tna", tna)

}

object Application {
  def gen: Gen[Application] = (
    Gen.oneOf("web", "mob", "pc", "srv", "app", "tv", "cnsl", "iot"),
    genStringOpt("aid", 10),
//    genStringOpt("evn", 10),
    genStringOpt("tna", 10)
    ).mapN(Application.apply)
}

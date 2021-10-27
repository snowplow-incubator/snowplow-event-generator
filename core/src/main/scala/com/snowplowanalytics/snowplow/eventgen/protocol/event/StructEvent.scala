package com.snowplowanalytics.snowplow.eventgen.protocol.event

import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.base._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._


final case class StructEvent(
                              se_ca: Option[String], //	se_category
                              se_ac: Option[String], //	se_action
                              se_la: Option[String], //	se_label
                              se_pr: Option[String], //	se_property
                              se_va: Option[Double], //	se_value
                            ) extends BodyEvent {
  override def toProto: List[BasicNameValuePair] =
    asKV("se_ca", se_ca) ++
      asKV("se_ac", se_ac) ++
      asKV("se_la", se_la) ++
      asKV("se_pr", se_pr) ++
      asKV("se_va", se_va)
}

object StructEvent {
  def gen: Gen[StructEvent] = (
    genStringOpt("se_ca", 10),
    genStringOpt("se_ca", 10),
    genStringOpt("se_ca", 10),
    genStringOpt("se_ca", 10),
    genDblOpt
    ).mapN(StructEvent.apply)
}
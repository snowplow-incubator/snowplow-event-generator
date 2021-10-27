package com.snowplowanalytics.snowplow.eventgen.protocol.common

import com.snowplowanalytics.snowplow.eventgen.base._
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
  def gen: Gen[DateTime] = (genInstantOpt, genInstantOpt, genInstantOpt, genTzOpt).mapN(DateTime.apply)

  def genOpt: Gen[Option[DateTime]] = Gen.option(gen)
}

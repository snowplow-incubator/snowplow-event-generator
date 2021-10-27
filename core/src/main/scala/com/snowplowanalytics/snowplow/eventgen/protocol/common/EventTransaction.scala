package com.snowplowanalytics.snowplow.eventgen.protocol.common

import org.scalacheck.{Arbitrary, Gen}
import com.snowplowanalytics.snowplow.eventgen.base._
import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.cats.implicits._

import java.util.UUID

final case class EventTransaction(
                             tid: Option[Int], // txn_id
                             eid: Option[UUID] // event_id
                           ) extends Protocol {
  override def toProto: List[BasicNameValuePair] =
      asKV("tid", tid) ++
      asKV("eid", eid)

}

object EventTransaction {

  def gen: Gen[EventTransaction] = (
    genIntOpt,
    Gen.option(Arbitrary.arbUuid.arbitrary)).mapN(EventTransaction.apply)

  def genOpt: Gen[Option[EventTransaction]] = Gen.option(gen)
}

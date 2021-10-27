package com.snowplowanalytics.snowplow.eventgen.protocol.common

import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.primitives._
import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.cats.implicits._
import org.scalacheck.Gen
import org.scalacheck.rng.Seed

import java.util.UUID
import scala.util.Random

final case class EventTransaction(
                                   tid: Option[Int], // txn_id
                                   eid: Option[UUID] // event_id
                                 ) extends Protocol {
  override def toProto: List[BasicNameValuePair] =
    asKV("tid", tid) ++
      asKV("eid", eid)

}

object EventTransaction {

  lazy val etRng = new Random(10000L)

  def genDup(synProb: Float, synTotal: Int): Gen[EventTransaction] =
    (
      genIntOpt,
      Gen.option(Gen.uuid.withPerturb(in =>
        if (synProb == 0 | synTotal == 0)
          in
        else if (etRng.nextInt(10000) < (synProb * 10000))
          Seed(etRng.nextInt(synTotal).toLong)
        else
          in)
      )).mapN(EventTransaction.apply)

  def genDupOpt(synProb: Float, synTotal: Int): Gen[Option[EventTransaction]] = Gen.option(genDup(synProb, synTotal))

  def gen: Gen[EventTransaction] = (
    genIntOpt,
    Gen.option(Gen.uuid)).mapN(EventTransaction.apply)

  def genOpt: Gen[Option[EventTransaction]] = Gen.option(gen)
}

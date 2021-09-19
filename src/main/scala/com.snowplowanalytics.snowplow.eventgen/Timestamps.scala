package com.snowplowanalytics.snowplow.eventgen

import org.scalacheck.{Arbitrary, Gen}

import cats.implicits._

import java.time.{ Duration, Instant }

case class Timestamps(collector: Instant,
                      etl: Instant,
                      deviceCreated: Option[Instant],
                      deviceSent: Option[Instant],
                      refrSent: Option[Instant],
                      derived: Instant,
                      `true`: Option[Instant])

object Timestamps {
  /** Copy the derived_tstamp implementation from enrich */
  def derive(collector: Instant, created: Option[Instant], sent: Option[Instant], `true`: Option[Instant]): Instant =
    `true` match {
      case Some(t) => t
      case None =>
        (created, sent) match {
          case (Some(dc), Some(ds)) =>
            if (dc.isBefore(ds)) collector.minus(Duration.between(dc, ds))
            else collector
          case _ =>
            collector
        }
    }

  private val TwoWeeks = 1209600000L
  private val MinTimestamp = System.currentTimeMillis() - TwoWeeks
  private val MaxTimestamp = System.currentTimeMillis() + TwoWeeks

  implicit val instantArbitrary: Arbitrary[Instant] =
    Arbitrary {
      for {
        seconds <- Gen.chooseNum(MinTimestamp, MaxTimestamp)
        nanos <- Gen.chooseNum(Instant.MIN.getNano, Instant.MAX.getNano)
      } yield Instant.ofEpochMilli(seconds).plusNanos(nanos.toLong)
    }

  val instantGen: Gen[Instant] =
    Arbitrary.arbitrary[Instant]

  val gen: Gen[Timestamps] =
    for {
      `true`        <- Arbitrary.arbitrary[Instant]
      created       <- Gen.option(Arbitrary.arbitrary[Instant])
      sentDiffMilli <- Gen.choose(10, 60000)
      sent          <- Gen.oneOf(Gen.const(created.map(_.plusMillis(sentDiffMilli.toLong))), Gen.option(Arbitrary.arbitrary[Instant]))
      collector     <- sent match {
          case Some(fromSent) => Gen.choose(10, 3000).map(_.toLong).map(fromSent.plusMillis)
          case None => Arbitrary.arbitrary[Instant]
        }
      etlDiffMilli  <- Gen.choose(10, 60000)
      etl            = collector.plusMillis(etlDiffMilli.toLong)
      derived        = Timestamps.derive(collector, created, sent, `true`.some)
    } yield Timestamps(collector, etl, created, sent, None, derived, `true`.some)
}



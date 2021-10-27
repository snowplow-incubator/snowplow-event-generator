package com.snowplowanalytics.snowplow.eventgen.collector

import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.base.{Url, genUserAgent}
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._

import java.util.UUID


case class Headers(
                    ref: Option[Url],
                    ua: Option[String],
                    cookie: Option[UUID]
                    // other header types could be added
                  ) {
  def toList: List[String] = List(
    ref.map(u => s"referrer: ${u.toString}"),
    ua.map(ua => s"user-agent: $ua"),
    cookie.map(cookie => s"cookie: $cookie")
  ).flatten
}

object Headers {
  def gen: Gen[Headers] = (Url.genOpt, Gen.option(genUserAgent), Gen.option(Gen.uuid)
    ).mapN(Headers.apply)
}

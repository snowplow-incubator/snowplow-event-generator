package com.snowplowanalytics.snowplow.eventgen.collector

import cats.implicits._

import org.scalacheck.Gen
import org.scalacheck.cats.implicits._

/**
 * Define the vendor and version of the payload, defined by collector endpoint
 */
final case class Api(vendor: String, version: String) {
  override def toString: String = if (vendor == "com.snowplowanalytics.snowplow" && version == "tp1") "/i" else s"$vendor/$version"
}

object Api {
  def genApi(nEvents: Int): Gen[Api] = (nEvents match {
    case 1 => (Gen.const("com.snowplowanalytics.snowplow"), Gen.oneOf("tp1", "tp2"))
    case _ => (Gen.const("com.snowplowanalytics.snowplow"), Gen.const("tp2"))
  }).mapN(Api.apply)
}
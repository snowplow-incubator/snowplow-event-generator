package com.snowplowanalytics.snowplow.eventgen.collector

import org.scalacheck.Gen

/**
 * Unambiguously identifies the collector source of this input line.
 *
 * @param name     kind and version of the collector (e.g. ssc-1.0.1-kafka)
 * @param encoding usually "UTF-8"
 * @param hostname the actual host the collector was running on
 */
final case class Source(
                         name: String,
                         encoding: String,
                         hostname: Option[String]
                       )

object Source {
  val name = "scala-tracker_1.0.0"
  val encoding = "UTF8"
  val hostname = Some("example.acme")
  val gen = Gen.const(Source(name, encoding, hostname))
}
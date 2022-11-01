/*
 * Copyright (c) 2021-2022 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.eventgen.collector

import org.scalacheck.Gen

/** Unambiguously identifies the collector source of this input line.
  *
  * @param name
  *   kind and version of the collector (e.g. ssc-1.0.1-kafka)
  * @param encoding
  *   usually "UTF-8"
  * @param hostname
  *   the actual host the collector was running on
  */
final case class Source(
  name: String,
  encoding: String,
  hostname: Option[String]
)

object Source {
  val name     = "scala-tracker_1.0.0"
  val encoding = "UTF8"
  val hostname = Some("example.acme")
  val gen      = Gen.const(Source(name, encoding, hostname))
}

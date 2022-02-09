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

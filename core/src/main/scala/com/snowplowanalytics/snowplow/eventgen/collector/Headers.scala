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
import com.snowplowanalytics.snowplow.eventgen.primitives.{Url, genUserAgent}
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

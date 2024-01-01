/*
 * Copyright (c) 2021-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.eventgen.protocol.enrichment

import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._

case class UrlEnrichment(
  page_urlquery: Option[String],
  page_urlfragment: Option[String],
)

object UrlEnrichment {
  def gen: Gen[UrlEnrichment] =
    (
      genStringOpt("page_urlquery", 10),
      genStringOpt("page_urlfragment", 10)
    ).mapN(UrlEnrichment.apply)
}

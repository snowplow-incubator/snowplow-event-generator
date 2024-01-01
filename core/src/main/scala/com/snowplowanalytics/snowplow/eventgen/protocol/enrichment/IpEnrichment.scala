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

case class IpEnrichment(
  geo_country: Option[String],
  geo_region: Option[String],
  geo_city: Option[String],
  geo_zipcode: Option[String],
  geo_latitude: Option[Double],
  geo_longitude: Option[Double],
  geo_region_name: Option[String],
  geo_timezone: Option[String],
  ip_isp: Option[String],
  ip_organization: Option[String],
  ip_domain: Option[String],
  ip_netspeed: Option[String],
)

object IpEnrichment {
  def gen: Gen[IpEnrichment] =
    (
      genStringOpt("geo_country", 10),
      genStringOpt("geo_region", 10),
      genStringOpt("geo_city", 10),
      genStringOpt("geo_zipcode", 10),
      genDblOpt,
      genDblOpt,
      genStringOpt("geo_region_name", 10),
      genStringOpt("geo_timezone", 10),
      genStringOpt("ip_isp", 10),
      genStringOpt("ip_organization", 10),
      genStringOpt("ip_domain", 10),
      genStringOpt("ip_netspeed", 10),
    ).mapN(IpEnrichment.apply)
}

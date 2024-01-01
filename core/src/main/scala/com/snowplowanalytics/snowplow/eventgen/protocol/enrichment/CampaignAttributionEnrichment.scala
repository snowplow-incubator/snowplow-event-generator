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

case class CampaignAttributionEnrichment(
  mkt_medium: Option[String],
  mkt_source: Option[String],
  mkt_term: Option[String],
  mkt_content: Option[String],
  mkt_campaign: Option[String],
  mkt_clickid: Option[String],
  mkt_network: Option[String]
)

object CampaignAttributionEnrichment {
  def gen: Gen[CampaignAttributionEnrichment] =
    (
      genStringOpt("mkt_medium", 10),
      genStringOpt("mkt_source", 10),
      genStringOpt("mkt_term", 10),
      genStringOpt("mkt_content", 10),
      genStringOpt("mkt_campaign", 10),
      genStringOpt("mkt_clickid", 10),
      genStringOpt("mkt_network", 10),
    ).mapN(CampaignAttributionEnrichment.apply)
}

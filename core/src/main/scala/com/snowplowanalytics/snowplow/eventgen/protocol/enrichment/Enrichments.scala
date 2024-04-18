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
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._

final case class Enrichments(
  defaultEnrichment: DefaultEnrichment,
  ipEnrichment: Option[IpEnrichment],
  urlEnrichment: Option[UrlEnrichment],
  refererEnrichment: Option[RefererEnrichment],
  campaignAttributionEnrichment: Option[CampaignAttributionEnrichment],
  currencyConversionEnrichment: Option[CurrencyConversionEnrichment],
  crossDomainEnrichment: Option[CrossDomainEnrichment],
  eventFingerprintEnrichment: Option[EventFingerprintEnrichment],
  deprecatedFields: Option[DeprecatedFields]
)

object Enrichments {

  def gen: Gen[Enrichments] =
    (
      DefaultEnrichment.gen,
      Gen.option(IpEnrichment.gen),
      Gen.option(UrlEnrichment.gen),
      Gen.option(RefererEnrichment.gen),
      Gen.option(CampaignAttributionEnrichment.gen),
      Gen.option(CurrencyConversionEnrichment.gen),
      Gen.option(CrossDomainEnrichment.gen),
      Gen.option(EventFingerprintEnrichment.gen),
      Gen.option(DeprecatedFields.gen)
    ).mapN(Enrichments.apply)
}

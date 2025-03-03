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

case class CurrencyConversionEnrichment(
  tr_total_base: Option[Double],
  tr_tax_base: Option[Double],
  tr_shipping_base: Option[Double],
  ti_price_base: Option[Double],
  base_currency: Option[String]
)

object CurrencyConversionEnrichment {
  def gen: Gen[CurrencyConversionEnrichment] =
    (
      genScale2DoubleOpt,
      genScale2DoubleOpt,
      genScale2DoubleOpt,
      genScale2DoubleOpt,
      Gen.option(genCurrency)
    ).mapN(CurrencyConversionEnrichment.apply)
}

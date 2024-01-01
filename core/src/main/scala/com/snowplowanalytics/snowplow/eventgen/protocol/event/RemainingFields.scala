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
package com.snowplowanalytics.snowplow.eventgen.protocol.event

import java.time.Instant
import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen

/*
 * Event fields that aren't populated in other parts
 */
final case class RemainingFields(
  se_ca: Option[String], //	se_category
  se_ac: Option[String], //	se_action
  se_la: Option[String], //	se_label
  se_pr: Option[String], //	se_property
  se_va: Option[Double], //	se_value
  geo_country: Option[String],
  geo_region: Option[String],
  geo_city: Option[String],
  geo_zipcode: Option[String],
  geo_latitude: Option[Double],
  geo_longitude: Option[Double],
  geo_region_name: Option[String],
  ip_isp: Option[String],
  ip_organization: Option[String],
  ip_domain: Option[String],
  ip_netspeed: Option[String],
  page_urlquery: Option[String],
  page_urlfragment: Option[String],
  refr_urlquery: Option[String],
  refr_urlfragment: Option[String],
  refr_medium: Option[String],
  refr_source: Option[String],
  refr_term: Option[String],
  mkt_medium: Option[String],
  mkt_source: Option[String],
  mkt_term: Option[String],
  mkt_content: Option[String],
  mkt_campaign: Option[String],
  tr_orderid: Option[String],
  tr_affiliation: Option[String],
  tr_total: Option[Double],
  tr_tax: Option[Double],
  tr_shipping: Option[Double],
  tr_city: Option[String],
  tr_state: Option[String],
  tr_country: Option[String],
  ti_orderid: Option[String],
  ti_sku: Option[String],
  ti_name: Option[String],
  ti_category: Option[String],
  ti_price: Option[Double],
  ti_quantity: Option[Int],
  br_name: Option[String],
  br_family: Option[String],
  br_version: Option[String],
  br_type: Option[String],
  br_renderengine: Option[String],
  os_name: Option[String],
  os_family: Option[String],
  os_manufacturer: Option[String],
  dvce_type: Option[String],
  dvce_ismobile: Option[Boolean],
  tr_currency: Option[String],
  tr_total_base: Option[Double],
  tr_tax_base: Option[Double],
  tr_shipping_base: Option[Double],
  ti_currency: Option[String],
  ti_price_base: Option[Double],
  base_currency: Option[String],
  geo_timezone: Option[String],
  mkt_clickid: Option[String],
  mkt_network: Option[String],
  etl_tags: Option[String],
  refr_domain_userid: Option[String],
  refr_dvce_tstamp: Option[Instant],
  derived_tstamp: Option[Instant],
  event_fingerprint: Option[String],
  etl_tstamp: Option[Instant]
) extends BodyEvent {
  override def toProto: List[BasicNameValuePair] =
    asKV("se_ca", se_ca) ++
      asKV("se_ac", se_ac) ++
      asKV("se_la", se_la) ++
      asKV("se_pr", se_pr) ++
      asKV("se_va", se_va)
}

object RemainingFields {
  def gen: Gen[RemainingFields] =
    for {
      se_category        <- genStringOpt("se_ca", 10)
      se_action          <- genStringOpt("se_ac", 10)
      se_label           <- genStringOpt("se_la", 10)
      se_property        <- genStringOpt("se_pr", 10)
      se_value           <- genDblOpt
      geo_country        <- genStringOpt("geo_country", 10)
      geo_region         <- genStringOpt("geo_region", 10)
      geo_city           <- genStringOpt("geo_city", 10)
      geo_zipcode        <- genStringOpt("geo_zipcode", 10)
      geo_latitude       <- genDblOpt
      geo_longitude      <- genDblOpt
      geo_region_name    <- genStringOpt("geo_region_name", 10)
      ip_isp             <- genStringOpt("ip_isp", 10)
      ip_organization    <- genStringOpt("ip_organization", 10)
      ip_domain          <- genStringOpt("ip_domain", 10)
      ip_netspeed        <- genStringOpt("ip_netspeed", 10)
      page_urlquery      <- genStringOpt("page_urlquery", 10)
      page_urlfragment   <- genStringOpt("page_urlfragment", 10)
      refr_urlquery      <- genStringOpt("refr_urlquery", 10)
      refr_urlfragment   <- genStringOpt("refr_urlfragment", 10)
      refr_medium        <- genStringOpt("refr_medium", 10)
      refr_source        <- genStringOpt("refr_source", 10)
      refr_term          <- genStringOpt("refr_term", 10)
      mkt_medium         <- genStringOpt("mkt_medium", 10)
      mkt_source         <- genStringOpt("mkt_source", 10)
      mkt_term           <- genStringOpt("mkt_term", 10)
      mkt_content        <- genStringOpt("mkt_content", 10)
      mkt_campaign       <- genStringOpt("mkt_campaign", 10)
      tr_orderid         <- genStringOpt("tr_orderid", 10)
      tr_affiliation     <- genStringOpt("tr_affiliation", 10)
      tr_total           <- genScale2Double
      tr_tax             <- genScale2Double
      tr_shipping        <- genScale2Double
      tr_city            <- genStringOpt("tr_city", 10)
      tr_state           <- genStringOpt("tr_state", 10)
      tr_country         <- genStringOpt("tr_country", 10)
      ti_orderid         <- genStringOpt("ti_orderid", 10)
      ti_sku             <- genStringOpt("ti_sku", 10)
      ti_name            <- genStringOpt("ti_name", 10)
      ti_category        <- genStringOpt("ti_category", 10)
      ti_price           <- genScale2Double
      ti_quantity        <- genIntOpt
      br_name            <- genStringOpt("br_name", 10)
      br_family          <- genStringOpt("br_family", 10)
      br_version         <- genStringOpt("br_version", 10)
      br_type            <- genStringOpt("br_type", 10)
      br_renderengine    <- genStringOpt("br_renderengine", 10)
      os_name            <- genStringOpt("os_name", 10)
      os_family          <- genStringOpt("os_family", 10)
      os_manufacturer    <- genStringOpt("os_manufacturer", 10)
      dvce_type          <- genStringOpt("dvce_type", 10)
      dvce_ismobile      <- genBoolOpt
      tr_currency        <- genStringOpt("tr_currency", 10)
      tr_total_base      <- genScale2Double
      tr_tax_base        <- genScale2Double
      tr_shipping_base   <- genScale2Double
      ti_currency        <- genStringOpt("ti_currency", 10)
      ti_price_base      <- genScale2Double
      base_currency      <- genStringOpt("base_currency", 10)
      geo_timezone       <- genStringOpt("geo_timezone", 10)
      mkt_clickid        <- genStringOpt("mkt_clickid", 10)
      mkt_network        <- genStringOpt("mkt_network", 10)
      etl_tags           <- genStringOpt("etl_tags", 10)
      refr_domain_userid <- genStringOpt("refr_domain_userid", 10)
      refr_dvce_tstamp   <- genInstantOpt(Instant.now)
      derived_tstamp     <- genInstantOpt(Instant.now)
      event_fingerprint  <- genStringOpt("event_fingerprint", 10)
      etl_tstamp         <- genInstantOpt(Instant.now)
    } yield RemainingFields(
      se_ca = se_category,
      se_ac = se_action,
      se_la = se_label,
      se_pr = se_property,
      se_va = se_value,
      geo_country = geo_country,
      geo_region = geo_region,
      geo_city = geo_city,
      geo_zipcode = geo_zipcode,
      geo_latitude = geo_latitude,
      geo_longitude = geo_longitude,
      geo_region_name = geo_region_name,
      ip_isp = ip_isp,
      ip_organization = ip_organization,
      ip_domain = ip_domain,
      ip_netspeed = ip_netspeed,
      page_urlquery = page_urlquery,
      page_urlfragment = page_urlfragment,
      refr_urlquery = refr_urlquery,
      refr_urlfragment = refr_urlfragment,
      refr_medium = refr_medium,
      refr_source = refr_source,
      refr_term = refr_term,
      mkt_medium = mkt_medium,
      mkt_source = mkt_source,
      mkt_term = mkt_term,
      mkt_content = mkt_content,
      mkt_campaign = mkt_campaign,
      tr_orderid = tr_orderid,
      tr_affiliation = tr_affiliation,
      tr_total = tr_total,
      tr_tax = tr_tax,
      tr_shipping = tr_shipping,
      tr_city = tr_city,
      tr_state = tr_state,
      tr_country = tr_country,
      ti_orderid = ti_orderid,
      ti_sku = ti_sku,
      ti_name = ti_name,
      ti_category = ti_category,
      ti_price = ti_price,
      ti_quantity = ti_quantity,
      br_name = br_name,
      br_family = br_family,
      br_version = br_version,
      br_type = br_type,
      br_renderengine = br_renderengine,
      os_name = os_name,
      os_family = os_family,
      os_manufacturer = os_manufacturer,
      dvce_type = dvce_type,
      dvce_ismobile = dvce_ismobile,
      tr_currency = tr_currency,
      tr_total_base = tr_total_base,
      tr_tax_base = tr_tax_base,
      tr_shipping_base = tr_shipping_base,
      ti_currency = ti_currency,
      ti_price_base = ti_price_base,
      base_currency = base_currency,
      geo_timezone = geo_timezone,
      mkt_clickid = mkt_clickid,
      mkt_network = mkt_network,
      etl_tags = etl_tags,
      refr_domain_userid = refr_domain_userid,
      refr_dvce_tstamp = refr_dvce_tstamp,
      derived_tstamp = derived_tstamp,
      event_fingerprint = event_fingerprint,
      etl_tstamp = etl_tstamp
    )

  def genScale2Double: Gen[Option[Double]] = genIntOpt.map(_.map(_.doubleValue / 100))
}

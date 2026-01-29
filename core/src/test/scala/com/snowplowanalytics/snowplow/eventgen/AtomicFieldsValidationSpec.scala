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
package com.snowplowanalytics.snowplow.eventgen

import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments

import java.time.Instant

/** Validates that generated events have atomic fields within the length limits defined by Enrich. */
class AtomicFieldsValidationSpec extends Specification {
  import AtomicFieldsValidationSpec._

  "All generated events" should {
    Fragments.foreach(AtomicFieldLimits.toList) { case (fieldName, limit) =>
      s"have $fieldName within $limit characters" >> {
        val now       = Instant.now()
        val eventsOpt = Gen.listOfN(SamplesPerField, eventGen(now)).sample
        eventsOpt must beSome
        val events = eventsOpt.get
        val violations = events.flatMap { event =>
          extractField(event, fieldName).flatMap { value =>
            if (value.length > limit)
              Some(s"$fieldName: length ${value.length} exceeds limit $limit (value: ${value.take(50)}...)")
            else
              None
          }
        }
        violations must beEmpty
      }
    }
  }

  "Struct events" should {
    "have se_action populated" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, structEventGen(now)).sample
      eventsOpt must beSome
      val events = eventsOpt.get
      events.flatMap(_.se_action) must not(beEmpty)
    }
  }

  "Unstruct events" should {
    "have event_vendor, event_name, event_format, event_version populated" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, unstructEventGen(now)).sample
      eventsOpt must beSome
      val events = eventsOpt.get
      events.flatMap(_.event_vendor) must not(beEmpty)
      events.flatMap(_.event_name) must not(beEmpty)
      events.flatMap(_.event_format) must not(beEmpty)
      events.flatMap(_.event_version) must not(beEmpty)
    }
  }

  "Transaction events" should {
    "have tr_orderid populated" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, transactionEventGen(now)).sample
      eventsOpt must beSome
      val events = eventsOpt.get
      events.flatMap(_.tr_orderid) must not(beEmpty)
    }

    "have tr_currency as exactly 3 characters when present" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, transactionEventGen(now)).sample
      eventsOpt must beSome
      val events     = eventsOpt.get
      val currencies = events.flatMap(_.tr_currency)
      currencies must not(beEmpty)
      currencies.forall(_.length == 3) must beTrue
    }
  }

  "Transaction item events" should {
    "have ti_orderid and ti_sku populated" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, transactionItemEventGen(now)).sample
      eventsOpt must beSome
      val events = eventsOpt.get
      events.flatMap(_.ti_orderid) must not(beEmpty)
      events.flatMap(_.ti_sku) must not(beEmpty)
    }

    "have ti_currency as exactly 3 characters when present" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, transactionItemEventGen(now)).sample
      eventsOpt must beSome
      val events     = eventsOpt.get
      val currencies = events.flatMap(_.ti_currency)
      currencies must not(beEmpty)
      currencies.forall(_.length == 3) must beTrue
    }
  }

  "PageView and PagePing events" should {
    "have page_url populated" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, pageViewEventGen(now)).sample
      eventsOpt must beSome
      val events = eventsOpt.get
      events.flatMap(_.page_url) must not(beEmpty)
    }

    "have page_urlscheme as http or https" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, pageViewEventGen(now)).sample
      eventsOpt must beSome
      val events  = eventsOpt.get
      val schemes = events.flatMap(_.page_urlscheme)
      schemes must not(beEmpty)
      schemes.forall(s => s == "http" || s == "https") must beTrue
    }
  }

  "Fields with tight length constraints" should {
    "have geo_country as exactly 2 characters when present" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, eventGen(now)).sample
      eventsOpt must beSome
      val events    = eventsOpt.get
      val countries = events.flatMap(_.geo_country)
      countries.forall(_.length == 2) must beTrue
    }

    "have geo_region as at most 3 characters when present" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, eventGen(now)).sample
      eventsOpt must beSome
      val events  = eventsOpt.get
      val regions = events.flatMap(_.geo_region)
      regions.forall(_.length <= 3) must beTrue
    }

    "have base_currency as exactly 3 characters when present" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, eventGen(now)).sample
      eventsOpt must beSome
      val events     = eventsOpt.get
      val currencies = events.flatMap(_.base_currency)
      currencies.forall(_.length == 3) must beTrue
    }
  }

  "Common required fields" should {
    "always have event_id as valid UUID format (36 chars)" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, eventGen(now)).sample
      eventsOpt must beSome
      val events = eventsOpt.get
      events.map(_.event_id.toString).forall(_.length == 36) must beTrue
    }

    "always have platform populated" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, eventGen(now)).sample
      eventsOpt must beSome
      val events = eventsOpt.get
      events.flatMap(_.platform) must not(beEmpty)
    }

    "have platform as one of the valid values" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, eventGen(now)).sample
      eventsOpt must beSome
      val events         = eventsOpt.get
      val validPlatforms = Set("web", "mob", "pc", "srv", "app", "tv", "cnsl", "iot")
      events.flatMap(_.platform).forall(validPlatforms.contains) must beTrue
    }

    "always have event type populated" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, eventGen(now)).sample
      eventsOpt must beSome
      val events = eventsOpt.get
      events.flatMap(_.event) must not(beEmpty)
    }

    "have event type as one of the valid values" >> {
      val now       = Instant.now()
      val eventsOpt = Gen.listOfN(SamplesPerField, eventGen(now)).sample
      eventsOpt must beSome
      val events          = eventsOpt.get
      val validEventTypes = Set("struct", "unstruct", "page_view", "page_ping", "transaction", "transaction_item")
      events.flatMap(_.event).forall(validEventTypes.contains) must beTrue
    }
  }
}

object AtomicFieldsValidationSpec {

  val SamplesPerField = 1000

  // Atomic field limits from Enrich's reference.conf (atomicFieldsLimits)
  val AtomicFieldLimits: Map[String, Int] = Map(
    "app_id"             -> 255,
    "platform"           -> 255,
    "event"              -> 128,
    "event_id"           -> 36,
    "name_tracker"       -> 128,
    "v_tracker"          -> 100,
    "v_collector"        -> 100,
    "v_etl"              -> 100,
    "user_id"            -> 255,
    "user_ipaddress"     -> 128,
    "user_fingerprint"   -> 128,
    "domain_userid"      -> 128,
    "network_userid"     -> 128,
    "geo_country"        -> 2,
    "geo_region"         -> 3,
    "geo_city"           -> 75,
    "geo_zipcode"        -> 15,
    "geo_region_name"    -> 100,
    "ip_isp"             -> 100,
    "ip_organization"    -> 128,
    "ip_domain"          -> 128,
    "ip_netspeed"        -> 100,
    "page_url"           -> 10000,
    "page_title"         -> 2000,
    "page_referrer"      -> 10000,
    "page_urlscheme"     -> 16,
    "page_urlhost"       -> 255,
    "page_urlpath"       -> 3000,
    "page_urlquery"      -> 6000,
    "page_urlfragment"   -> 3000,
    "refr_urlscheme"     -> 16,
    "refr_urlhost"       -> 255,
    "refr_urlpath"       -> 6000,
    "refr_urlquery"      -> 6000,
    "refr_urlfragment"   -> 3000,
    "refr_medium"        -> 25,
    "refr_source"        -> 50,
    "refr_term"          -> 255,
    "mkt_clickid"        -> 1000,
    "mkt_network"        -> 64,
    "mkt_medium"         -> 255,
    "mkt_source"         -> 255,
    "mkt_term"           -> 255,
    "mkt_content"        -> 500,
    "mkt_campaign"       -> 255,
    "se_category"        -> 1000,
    "se_action"          -> 1000,
    "se_label"           -> 4096,
    "se_property"        -> 1000,
    "tr_orderid"         -> 255,
    "tr_affiliation"     -> 255,
    "tr_city"            -> 255,
    "tr_state"           -> 255,
    "tr_country"         -> 255,
    "ti_orderid"         -> 255,
    "ti_sku"             -> 255,
    "ti_name"            -> 255,
    "ti_category"        -> 255,
    "useragent"          -> 1000,
    "br_name"            -> 50,
    "br_family"          -> 50,
    "br_version"         -> 50,
    "br_type"            -> 50,
    "br_renderengine"    -> 50,
    "br_lang"            -> 255,
    "br_colordepth"      -> 12,
    "os_name"            -> 50,
    "os_family"          -> 50,
    "os_manufacturer"    -> 50,
    "os_timezone"        -> 255,
    "dvce_type"          -> 50,
    "doc_charset"        -> 128,
    "tr_currency"        -> 3,
    "ti_currency"        -> 3,
    "base_currency"      -> 3,
    "geo_timezone"       -> 64,
    "etl_tags"           -> 500,
    "refr_domain_userid" -> 128,
    "domain_sessionid"   -> 128,
    "event_vendor"       -> 1000,
    "event_name"         -> 1000,
    "event_format"       -> 128,
    "event_version"      -> 128,
    "event_fingerprint"  -> 128
  )

  private val defaultFrequencies = GenConfig.EventsFrequencies(
    struct = 1,
    unstruct = 1,
    pageView = 1,
    pagePing = 1,
    transaction = 1,
    transactionItem = 1,
    unstructEventFrequencyDefault = 1,
    unstructEventFrequencies = Map.empty
  )

  private val defaultEventsPerPayload = GenConfig.EventsPerPayload(min = 1, max = 1)
  private val defaultContextsPerEvent = GenConfig.ContextsPerEvent(min = 0, max = 3)
  private val defaultAppIds           = List("test-app-id")

  def eventGen(now: Instant): Gen[Event] =
    SdkEvent
      .gen(
        defaultEventsPerPayload,
        now,
        defaultFrequencies,
        defaultContextsPerEvent,
        generateEnrichments = true,
        GenConfig.IdentitySource.NoIdentity,
        None,
        defaultAppIds
      )
      .flatMap(events => Gen.oneOf(events))

  private def eventTypeFrequencies(eventType: String): GenConfig.EventsFrequencies =
    eventType match {
      case "struct" =>
        defaultFrequencies.copy(
          struct = 1,
          unstruct = 0,
          pageView = 0,
          pagePing = 0,
          transaction = 0,
          transactionItem = 0
        )
      case "unstruct" =>
        defaultFrequencies.copy(
          struct = 0,
          unstruct = 1,
          pageView = 0,
          pagePing = 0,
          transaction = 0,
          transactionItem = 0
        )
      case "pageView" =>
        defaultFrequencies.copy(
          struct = 0,
          unstruct = 0,
          pageView = 1,
          pagePing = 0,
          transaction = 0,
          transactionItem = 0
        )
      case "pagePing" =>
        defaultFrequencies.copy(
          struct = 0,
          unstruct = 0,
          pageView = 0,
          pagePing = 1,
          transaction = 0,
          transactionItem = 0
        )
      case "transaction" =>
        defaultFrequencies.copy(
          struct = 0,
          unstruct = 0,
          pageView = 0,
          pagePing = 0,
          transaction = 1,
          transactionItem = 0
        )
      case "transactionItem" =>
        defaultFrequencies.copy(
          struct = 0,
          unstruct = 0,
          pageView = 0,
          pagePing = 0,
          transaction = 0,
          transactionItem = 1
        )
      case _ => defaultFrequencies
    }

  def structEventGen(now: Instant): Gen[Event] =
    SdkEvent
      .gen(
        defaultEventsPerPayload,
        now,
        eventTypeFrequencies("struct"),
        defaultContextsPerEvent,
        generateEnrichments = true,
        GenConfig.IdentitySource.NoIdentity,
        None,
        defaultAppIds
      )
      .flatMap(events => Gen.oneOf(events))

  def unstructEventGen(now: Instant): Gen[Event] =
    SdkEvent
      .gen(
        defaultEventsPerPayload,
        now,
        eventTypeFrequencies("unstruct"),
        defaultContextsPerEvent,
        generateEnrichments = true,
        GenConfig.IdentitySource.NoIdentity,
        None,
        defaultAppIds
      )
      .flatMap(events => Gen.oneOf(events))

  def pageViewEventGen(now: Instant): Gen[Event] =
    SdkEvent
      .gen(
        defaultEventsPerPayload,
        now,
        eventTypeFrequencies("pageView"),
        defaultContextsPerEvent,
        generateEnrichments = true,
        GenConfig.IdentitySource.NoIdentity,
        None,
        defaultAppIds
      )
      .flatMap(events => Gen.oneOf(events))

  def pagePingEventGen(now: Instant): Gen[Event] =
    SdkEvent
      .gen(
        defaultEventsPerPayload,
        now,
        eventTypeFrequencies("pagePing"),
        defaultContextsPerEvent,
        generateEnrichments = true,
        GenConfig.IdentitySource.NoIdentity,
        None,
        defaultAppIds
      )
      .flatMap(events => Gen.oneOf(events))

  def transactionEventGen(now: Instant): Gen[Event] =
    SdkEvent
      .gen(
        defaultEventsPerPayload,
        now,
        eventTypeFrequencies("transaction"),
        defaultContextsPerEvent,
        generateEnrichments = true,
        GenConfig.IdentitySource.NoIdentity,
        None,
        defaultAppIds
      )
      .flatMap(events => Gen.oneOf(events))

  def transactionItemEventGen(now: Instant): Gen[Event] =
    SdkEvent
      .gen(
        defaultEventsPerPayload,
        now,
        eventTypeFrequencies("transactionItem"),
        defaultContextsPerEvent,
        generateEnrichments = true,
        GenConfig.IdentitySource.NoIdentity,
        None,
        defaultAppIds
      )
      .flatMap(events => Gen.oneOf(events))

  def extractField(event: Event, fieldName: String): Option[String] =
    fieldName match {
      case "app_id"             => event.app_id
      case "platform"           => event.platform
      case "event"              => event.event
      case "event_id"           => Some(event.event_id.toString)
      case "name_tracker"       => event.name_tracker
      case "v_tracker"          => event.v_tracker
      case "v_collector"        => Some(event.v_collector)
      case "v_etl"              => Some(event.v_etl)
      case "user_id"            => event.user_id
      case "user_ipaddress"     => event.user_ipaddress
      case "user_fingerprint"   => event.user_fingerprint
      case "domain_userid"      => event.domain_userid
      case "network_userid"     => event.network_userid
      case "geo_country"        => event.geo_country
      case "geo_region"         => event.geo_region
      case "geo_city"           => event.geo_city
      case "geo_zipcode"        => event.geo_zipcode
      case "geo_region_name"    => event.geo_region_name
      case "ip_isp"             => event.ip_isp
      case "ip_organization"    => event.ip_organization
      case "ip_domain"          => event.ip_domain
      case "ip_netspeed"        => event.ip_netspeed
      case "page_url"           => event.page_url
      case "page_title"         => event.page_title
      case "page_referrer"      => event.page_referrer
      case "page_urlscheme"     => event.page_urlscheme
      case "page_urlhost"       => event.page_urlhost
      case "page_urlpath"       => event.page_urlpath
      case "page_urlquery"      => event.page_urlquery
      case "page_urlfragment"   => event.page_urlfragment
      case "refr_urlscheme"     => event.refr_urlscheme
      case "refr_urlhost"       => event.refr_urlhost
      case "refr_urlpath"       => event.refr_urlpath
      case "refr_urlquery"      => event.refr_urlquery
      case "refr_urlfragment"   => event.refr_urlfragment
      case "refr_medium"        => event.refr_medium
      case "refr_source"        => event.refr_source
      case "refr_term"          => event.refr_term
      case "mkt_clickid"        => event.mkt_clickid
      case "mkt_network"        => event.mkt_network
      case "mkt_medium"         => event.mkt_medium
      case "mkt_source"         => event.mkt_source
      case "mkt_term"           => event.mkt_term
      case "mkt_content"        => event.mkt_content
      case "mkt_campaign"       => event.mkt_campaign
      case "se_category"        => event.se_category
      case "se_action"          => event.se_action
      case "se_label"           => event.se_label
      case "se_property"        => event.se_property
      case "tr_orderid"         => event.tr_orderid
      case "tr_affiliation"     => event.tr_affiliation
      case "tr_city"            => event.tr_city
      case "tr_state"           => event.tr_state
      case "tr_country"         => event.tr_country
      case "ti_orderid"         => event.ti_orderid
      case "ti_sku"             => event.ti_sku
      case "ti_name"            => event.ti_name
      case "ti_category"        => event.ti_category
      case "useragent"          => event.useragent
      case "br_name"            => event.br_name
      case "br_family"          => event.br_family
      case "br_version"         => event.br_version
      case "br_type"            => event.br_type
      case "br_renderengine"    => event.br_renderengine
      case "br_lang"            => event.br_lang
      case "br_colordepth"      => event.br_colordepth
      case "os_name"            => event.os_name
      case "os_family"          => event.os_family
      case "os_manufacturer"    => event.os_manufacturer
      case "os_timezone"        => event.os_timezone
      case "dvce_type"          => event.dvce_type
      case "doc_charset"        => event.doc_charset
      case "tr_currency"        => event.tr_currency
      case "ti_currency"        => event.ti_currency
      case "base_currency"      => event.base_currency
      case "geo_timezone"       => event.geo_timezone
      case "etl_tags"           => event.etl_tags
      case "refr_domain_userid" => event.refr_domain_userid
      case "domain_sessionid"   => event.domain_sessionid
      case "event_vendor"       => event.event_vendor
      case "event_name"         => event.event_name
      case "event_format"       => event.event_format
      case "event_version"      => event.event_version
      case "event_fingerprint"  => event.event_fingerprint
      case _                    => None
    }
}

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
package com.snowplowanalytics.snowplow.eventgen.enrich

import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{Contexts, UnstructEvent}
import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload
import com.snowplowanalytics.snowplow.eventgen.protocol.Body
import com.snowplowanalytics.snowplow.eventgen.protocol.common.Web
import com.snowplowanalytics.snowplow.eventgen.protocol.event.{
  EventFrequencies,
  EventType,
  LegacyEvent,
  PagePing,
  PageView,
  RemainingFields,
  StructEvent,
  UnstructEventWrapper
}
import io.circe.Json
import org.scalacheck.Gen

import java.time.Instant
import java.util.UUID

object SdkEvent {
  private def extractWeb[A](el: Body, acc: Web => Option[A]): Option[A] = el.event match {
    case event: LegacyEvent =>
      event match {
        case pp: PagePing =>
          pp.deps.headOption.flatMap {
            case w: Web => acc(w)
            case _      => None
          }
        case pp: PageView =>
          pp.deps.headOption.flatMap { w: Web =>
            acc(w)
          }
        case _ => None
      }
    case _ => None
  }

  private def eventFromColPayload(p: CollectorPayload, fallbackEid: UUID): List[Event] =
    p.payload.map { el =>
      val evnt = Some(el.e match {
        case EventType.Struct | EventType.RemainingFields => "struct"
        case EventType.Unstruct => "unstruct"
        case EventType.PageView => "page_view"
        case EventType.PagePing => "page_ping"
      })

      val (ue, eName, ueVendor, ueFormat, ueVersion) = el.event match {
        case UnstructEventWrapper(event, _) =>
          val sk = event.schema
          (event.toUnstructEvent, Some(sk.name), Some(sk.vendor), Some(sk.format), Some(sk.version.asString))
        case _ =>
          (UnstructEvent(data = None), evnt, None, None, None)
      }

      val remainingFieldsOpt = el.event match {
        case a: RemainingFields => Some(a)
        case _                  => None
      }

      val structEventOpt = el.event match {
        case s: StructEvent => Some(s)
        case _              => None
      }

      val pagePingOpt = el.event match {
        case p: PagePing => Some(p)
        case _           => None
      }

      Event(
        app_id = el.app.aid,
        platform = Some(el.app.p),
        collector_tstamp = p.context.timestamp,
        dvce_created_tstamp = el.dt.flatMap(_.dtm),
        event = evnt,
        event_id = el.et.eid.getOrElse(fallbackEid),
        txn_id = el.et.tid,
        name_tracker = el.app.tna,
        v_tracker = Some(el.tv.tv),
        v_collector = p.source.name,
        v_etl = "v_etl",
        user_id = el.u.flatMap(_.uid),
        user_ipaddress = el.u.flatMap(_.ip).orElse(p.context.ipAddress.map(_.toString)),
        user_fingerprint = extractWeb(el, _.fp.map(_.toString)),
        domain_userid = el.u.flatMap(_.duid),
        domain_sessionidx = el.u.flatMap(_.vid),
        network_userid = el
          .u
          .flatMap(_.tnuid)
          .orElse(el.u.flatMap(_.nuid))
          .orElse(p.context.userId)
          .orElse(p.context.headers.cookie)
          .map(_.toString),
        geo_country = remainingFieldsOpt.flatMap(_.geo_country),
        geo_region = remainingFieldsOpt.flatMap(_.geo_region),
        geo_city = remainingFieldsOpt.flatMap(_.geo_city),
        geo_zipcode = remainingFieldsOpt.flatMap(_.geo_zipcode),
        geo_latitude = remainingFieldsOpt.flatMap(_.geo_latitude),
        geo_longitude = remainingFieldsOpt.flatMap(_.geo_longitude),
        geo_region_name = remainingFieldsOpt.flatMap(_.geo_region_name),
        ip_isp = remainingFieldsOpt.flatMap(_.ip_isp),
        ip_organization = remainingFieldsOpt.flatMap(_.ip_organization),
        ip_domain = remainingFieldsOpt.flatMap(_.ip_domain),
        ip_netspeed = remainingFieldsOpt.flatMap(_.ip_netspeed),
        page_url = extractWeb(el, _.url.map(_.toString)),
        page_title = extractWeb(el, _.page),
        page_referrer = extractWeb(el, _.refr.map(_.toString)),
        page_urlscheme = extractWeb(el, _.url.map(_.scheme)),
        page_urlhost = extractWeb(el, _.url.map(_.host)),
        page_urlport = extractWeb(el, _.url.map(_.sdkPort)),
        page_urlpath = extractWeb(el, _.url.map(_.path)),
        page_urlquery = remainingFieldsOpt.flatMap(_.page_urlquery),
        page_urlfragment = remainingFieldsOpt.flatMap(_.page_urlfragment),
        refr_urlscheme = extractWeb(el, _.refr.map(_.scheme)),
        refr_urlhost = extractWeb(el, _.refr.map(_.host)),
        refr_urlport = extractWeb(el, _.refr.map(_.sdkPort)),
        refr_urlpath = extractWeb(el, _.refr.map(_.path)),
        refr_urlquery = remainingFieldsOpt.flatMap(_.refr_urlquery),
        refr_urlfragment = remainingFieldsOpt.flatMap(_.refr_urlfragment),
        refr_medium = remainingFieldsOpt.flatMap(_.refr_medium),
        refr_source = remainingFieldsOpt.flatMap(_.refr_source),
        refr_term = remainingFieldsOpt.flatMap(_.refr_term),
        mkt_medium = remainingFieldsOpt.flatMap(_.mkt_medium),
        mkt_source = remainingFieldsOpt.flatMap(_.mkt_source),
        mkt_term = remainingFieldsOpt.flatMap(_.mkt_term),
        mkt_content = remainingFieldsOpt.flatMap(_.mkt_content),
        mkt_campaign = remainingFieldsOpt.flatMap(_.mkt_campaign),
        contexts = el.context.map(_.contexts).getOrElse(Contexts(List.empty[SelfDescribingData[Json]])),
        se_category = structEventOpt.flatMap(_.se_ca),
        se_action = structEventOpt.flatMap(_.se_ac),
        se_label = structEventOpt.flatMap(_.se_la),
        se_property = structEventOpt.flatMap(_.se_pr),
        se_value = structEventOpt.flatMap(_.se_va),
        unstruct_event = ue,
        tr_orderid = remainingFieldsOpt.flatMap(_.tr_orderid),
        tr_affiliation = remainingFieldsOpt.flatMap(_.tr_affiliation),
        tr_total = remainingFieldsOpt.flatMap(_.tr_total),
        tr_tax = remainingFieldsOpt.flatMap(_.tr_tax),
        tr_shipping = remainingFieldsOpt.flatMap(_.tr_shipping),
        tr_city = remainingFieldsOpt.flatMap(_.tr_city),
        tr_state = remainingFieldsOpt.flatMap(_.tr_state),
        tr_country = remainingFieldsOpt.flatMap(_.tr_country),
        ti_orderid = remainingFieldsOpt.flatMap(_.ti_orderid),
        ti_sku = remainingFieldsOpt.flatMap(_.ti_sku),
        ti_name = remainingFieldsOpt.flatMap(_.ti_name),
        ti_category = remainingFieldsOpt.flatMap(_.ti_category),
        ti_price = remainingFieldsOpt.flatMap(_.ti_price),
        ti_quantity = remainingFieldsOpt.flatMap(_.ti_quantity),
        pp_xoffset_min = pagePingOpt.flatMap(_.pp_mix),
        pp_xoffset_max = pagePingOpt.flatMap(_.pp_max),
        pp_yoffset_min = pagePingOpt.flatMap(_.pp_miy),
        pp_yoffset_max = pagePingOpt.flatMap(_.pp_may),
        useragent = extractWeb(el, _.ua).orElse(p.context.headers.ua),
        br_name = remainingFieldsOpt.flatMap(_.br_name),
        br_family = remainingFieldsOpt.flatMap(_.br_family),
        br_version = remainingFieldsOpt.flatMap(_.br_version),
        br_type = remainingFieldsOpt.flatMap(_.br_type),
        br_renderengine = remainingFieldsOpt.flatMap(_.br_renderengine),
        br_lang = extractWeb(el, _.lang),
        br_features_pdf = extractWeb(el, _.f_pdf),
        br_features_flash = extractWeb(el, _.f_fla),
        br_features_java = extractWeb(el, _.f_java),
        br_features_director = extractWeb(el, _.f_dir),
        br_features_quicktime = extractWeb(el, _.f_qt),
        br_features_realplayer = extractWeb(el, _.f_realp),
        br_features_windowsmedia = extractWeb(el, _.f_wma),
        br_features_gears = extractWeb(el, _.f_gears),
        br_features_silverlight = extractWeb(el, _.f_ag),
        br_cookies = extractWeb(el, _.cookie),
        br_colordepth = extractWeb(el, _.cd).map(_.toString),
        br_viewwidth = extractWeb(el, _.vp.map(_.x)),
        br_viewheight = extractWeb(el, _.vp.map(_.y)),
        os_name = remainingFieldsOpt.flatMap(_.os_name),
        os_family = remainingFieldsOpt.flatMap(_.os_family),
        os_manufacturer = remainingFieldsOpt.flatMap(_.os_manufacturer),
        os_timezone = el.dt.flatMap(_.tz),
        dvce_type = remainingFieldsOpt.flatMap(_.dvce_type),
        dvce_ismobile = remainingFieldsOpt.flatMap(_.dvce_ismobile),
        dvce_screenwidth = el.dev.flatMap(_.res.map(_.x)),
        dvce_screenheight = el.dev.flatMap(_.res.map(_.y)),
        doc_charset = extractWeb(el, _.cs),
        doc_width = extractWeb(el, _.ds.map(_.x)),
        doc_height = extractWeb(el, _.ds.map(_.y)),
        tr_currency = remainingFieldsOpt.flatMap(_.tr_currency),
        tr_total_base = remainingFieldsOpt.flatMap(_.tr_total_base),
        tr_tax_base = remainingFieldsOpt.flatMap(_.tr_tax_base),
        tr_shipping_base = remainingFieldsOpt.flatMap(_.tr_shipping_base),
        ti_currency = remainingFieldsOpt.flatMap(_.ti_currency),
        ti_price_base = remainingFieldsOpt.flatMap(_.ti_price_base),
        base_currency = remainingFieldsOpt.flatMap(_.base_currency),
        geo_timezone = remainingFieldsOpt.flatMap(_.geo_timezone),
        mkt_clickid = remainingFieldsOpt.flatMap(_.mkt_clickid),
        mkt_network = remainingFieldsOpt.flatMap(_.mkt_network),
        etl_tags = remainingFieldsOpt.flatMap(_.etl_tags),
        dvce_sent_tstamp = el.dt.flatMap(_.dtm),
        refr_domain_userid = remainingFieldsOpt.flatMap(_.refr_domain_userid),
        refr_dvce_tstamp = remainingFieldsOpt.flatMap(_.refr_dvce_tstamp),
        derived_contexts = Contexts(List.empty[SelfDescribingData[Json]]),
        domain_sessionid = el.u.flatMap(_.sid.map(_.toString)),
        derived_tstamp = remainingFieldsOpt.flatMap(_.derived_tstamp),
        event_vendor = ueVendor,
        event_name = eName,
        event_format = ueFormat,
        event_version = ueVersion,
        event_fingerprint = remainingFieldsOpt.flatMap(_.event_fingerprint),
        true_tstamp = el.dt.flatMap(_.ttm),
        etl_tstamp = remainingFieldsOpt.flatMap(_.etl_tstamp)
      )
    }

  def gen(
    eventPerPayloadMin: Int,
    eventPerPayloadMax: Int,
    now: Instant,
    frequencies: EventFrequencies
  ): Gen[List[Event]] =
    genPair(eventPerPayloadMin, eventPerPayloadMax, now, frequencies).map(_._2)

  def genPairDup(
    natProb: Float,
    synProb: Float,
    natTotal: Int,
    synTotal: Int,
    eventPerPayloadMin: Int,
    eventPerPayloadMax: Int,
    now: Instant,
    frequencies: EventFrequencies
  ): Gen[(CollectorPayload, List[Event])] =
    for {
      cp <- CollectorPayload.genDup(
        natProb,
        synProb,
        natTotal,
        synTotal,
        eventPerPayloadMin,
        eventPerPayloadMax,
        now,
        frequencies
      )
      eid <- Gen.uuid
    } yield (cp, eventFromColPayload(cp, eid))

  def genPair(
    eventPerPayloadMin: Int,
    eventPerPayloadMax: Int,
    now: Instant,
    frequencies: EventFrequencies
  ): Gen[(CollectorPayload, List[Event])] =
    for {
      cp  <- CollectorPayload.gen(eventPerPayloadMin, eventPerPayloadMax, now, frequencies)
      eid <- Gen.uuid
    } yield (cp, eventFromColPayload(cp, eid))

}

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

      val evnt =  Some(el.e match {
          case EventType.Struct   => "struct"
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
        geo_country = None,
        geo_region = None,
        geo_city = None,
        geo_zipcode = None,
        geo_latitude = None,
        geo_longitude = None,
        geo_region_name = None,
        ip_isp = None,
        ip_organization = None,
        ip_domain = None,
        ip_netspeed = None,
        page_url = extractWeb(el, _.url.map(_.toString)),
        page_title = extractWeb(el, _.page),
        page_referrer = extractWeb(el, _.refr.map(_.toString)),
        page_urlscheme = extractWeb(el, _.url.map(_.scheme)),
        page_urlhost = extractWeb(el, _.url.map(_.host)),
        page_urlport = extractWeb(el, _.url.map(_.sdkPort)),
        page_urlpath = extractWeb(el, _.url.map(_.path)),
        page_urlquery = None,
        page_urlfragment = None,
        refr_urlscheme = extractWeb(el, _.refr.map(_.scheme)),
        refr_urlhost = extractWeb(el, _.refr.map(_.host)),
        refr_urlport = extractWeb(el, _.refr.map(_.sdkPort)),
        refr_urlpath = extractWeb(el, _.refr.map(_.path)),
        refr_urlquery = None,
        refr_urlfragment = None,
        refr_medium = None,
        refr_source = None,
        refr_term = None,
        mkt_medium = None,
        mkt_source = None,
        mkt_term = None,
        mkt_content = None,
        mkt_campaign = None,
        contexts = el.context.map(_.contexts).getOrElse(Contexts(List.empty[SelfDescribingData[Json]])),
        se_category = None,
        se_action = None,
        se_label = None,
        se_property = None,
        se_value = None,
        unstruct_event = ue,
        tr_orderid = None,
        tr_affiliation = None,
        tr_total = None,
        tr_tax = None,
        tr_shipping = None,
        tr_city = None,
        tr_state = None,
        tr_country = None,
        ti_orderid = None,
        ti_sku = None,
        ti_name = None,
        ti_category = None,
        ti_price = None,
        ti_quantity = None,
        pp_xoffset_min = None,
        pp_xoffset_max = None,
        pp_yoffset_min = None,
        pp_yoffset_max = None,
        useragent = extractWeb(el, _.ua).orElse(p.context.headers.ua),
        br_name = None,
        br_family = None,
        br_version = None,
        br_type = None,
        br_renderengine = None,
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
        os_name = None,
        os_family = None,
        os_manufacturer = None,
        os_timezone = el.dt.flatMap(_.tz),
        dvce_type = None,
        dvce_ismobile = None,
        dvce_screenwidth = el.dev.flatMap(_.res.map(_.x)),
        dvce_screenheight = el.dev.flatMap(_.res.map(_.y)),
        doc_charset = extractWeb(el, _.cs),
        doc_width = extractWeb(el, _.ds.map(_.x)),
        doc_height = extractWeb(el, _.ds.map(_.y)),
        tr_currency = None,
        tr_total_base = None,
        tr_tax_base = None,
        tr_shipping_base = None,
        ti_currency = None,
        ti_price_base = None,
        base_currency = None,
        geo_timezone = None,
        mkt_clickid = None,
        mkt_network = None,
        etl_tags = None,
        dvce_sent_tstamp = el.dt.flatMap(_.dtm),
        refr_domain_userid = None,
        refr_dvce_tstamp = None,
        derived_contexts = Contexts(List.empty[SelfDescribingData[Json]]),
        domain_sessionid = el.u.flatMap(_.sid.map(_.toString)),
        derived_tstamp = None,
        event_vendor = ueVendor,
        event_name = eName,
        event_format = ueFormat,
        event_version = ueVersion,
        event_fingerprint = None,
        true_tstamp = None,
        etl_tstamp = None
      )
    }

  def gen(eventPerPayloadMin: Int, eventPerPayloadMax: Int, now: Instant, frequencies: EventFrequencies): Gen[List[Event]] =
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
      cp  <- CollectorPayload.genDup(natProb, synProb, natTotal, synTotal, eventPerPayloadMin, eventPerPayloadMax, now, frequencies)
      eid <- Gen.uuid
    } yield (cp, eventFromColPayload(cp, eid))

  def genPair(eventPerPayloadMin: Int, eventPerPayloadMax: Int, now: Instant, frequencies: EventFrequencies): Gen[(CollectorPayload, List[Event])] =
    for {
      cp  <- CollectorPayload.gen(eventPerPayloadMin, eventPerPayloadMax, now, frequencies)
      eid <- Gen.uuid
    } yield (cp, eventFromColPayload(cp, eid))

}

/*
 * Copyright (c) 2021 Snowplow Analytics Ltd. All rights reserved.
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

import java.time.Instant
import java.util.UUID

import scala.util.Random

import cats.implicits._
import cats.effect.Sync

import fs2.Stream

import io.circe.Json
import io.circe.syntax.EncoderOps

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer, SelfDescribingData}

import org.scalacheck.{Arbitrary, Gen}

import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{ UnstructEvent, Contexts }
import com.snowplowanalytics.snowplow.eventgen.{ Timestamps, ContextsGen }

object EnrichedEventGen {

  private def emptyEvent(id: UUID, collectorTstamp: Instant, vCollector: String, vEtl: String): Event =
    Event(None, None, None, collectorTstamp, None, None, id, None, None, None, vCollector, vEtl, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      Contexts(Nil), None, None, None, None, None, UnstructEvent(None), None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None,
      Contexts(Nil), None, None, None, None, None, None, None, None)

  private val VCollector = "ssc-2.2.1-pubsub"
  private val VEtl = "beam-enrich-1.2.0-common-1.1.0"
  private val AppId = "spirit-walk"
  private val Platform = "web"
  private val NameTracker = "datacap"
  private val VTracker = "js-2.5.3-m1"
  private val UserId = "ada.blackjack@iglu.com"
  private val RefrUrlDomain = "google"
  private val RefrMedium = "search"
  private val RefrSource = "Google"
  private val MktMedium = "email"
  private val MktSource = "openemail"
  private val MktCampaign = "igloosforall"
  private val Useragent =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/601.1.56 (KHTML, like Gecko) Version/9.0 Safari/601.1.56"
  private val EventFormat = "jsonschema"
  private val WebPageSchemaKey = SchemaKey("com.snowplowanalytics.snowplow", "web_page", "jsonschema", SchemaVer.Full(1, 0, 0))
  private val GeoLocationSchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow", "geolocation_context", "jsonschema", SchemaVer.Full(1, 0, 0))
  private val LinkClickSchemaKey = SchemaKey("com.snowplowanalytics.snowplow", "link_click", "jsonschema", SchemaVer.Full(1, 0, 1))
  private val PerfTimingSchemaKey = SchemaKey("org.w3", "PerformanceTiming", "jsonschema", SchemaVer.Full(1, 0, 0))
  private val PerfTimingSchemaProps = List(
    "navigationStart",
    "redirectStart",
    "redirectEnd",
    "fetchStart",
    "domainLookupStart",
    "domainLookupEnd",
    "connectStart",
    "secureConnectionStart",
    "connectEnd",
    "requestStart",
    "responseStart",
    "responseEnd",
    "unloadEventStart",
    "unloadEventEnd",
    "domLoading",
    "domInteractive",
    "domContentLoadedEventStart",
    "domContentLoadedEventEnd",
    "domComplete",
    "loadEventStart",
    "loadEventEnd",
    "msFirstPaint",
    "chromeFirstPaint",
    "requestEnd",
    "proxyStart",
    "proxyEnd"
  )

  private val LoremIpsum =
    """Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
      | Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure
      | dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
      | proident, sunt in culpa qui officia deserunt mollit anim id est laborum.""".stripMargin
      .replaceAll("""[\p{Punct}]""", "")
      .toLowerCase
      .split(" ")
      .toList

  // private val maxTimestamp = Instant.now().getEpochSecond + (30 * 24 * 60 * 60).toLong // 1 month
  // private val minTimestamp = Instant.now().getEpochSecond - (30 * 24 * 60 * 60).toLong // 1 month


  private val ipv4Address: Gen[String] =
    for {
      a <- Gen.chooseNum(0, 255)
      b <- Gen.chooseNum(0, 255)
      c <- Gen.chooseNum(0, 255)
      d <- Gen.chooseNum(0, 255)
    } yield s"$a.$b.$c.$d"

  private val ipv6Address: Gen[String] =
    for {
      a <- Arbitrary.arbitrary[Short]
      b <- Arbitrary.arbitrary[Short]
      c <- Arbitrary.arbitrary[Short]
      d <- Arbitrary.arbitrary[Short]
      e <- Arbitrary.arbitrary[Short]
      f <- Arbitrary.arbitrary[Short]
      g <- Arbitrary.arbitrary[Short]
      h <- Arbitrary.arbitrary[Short]
    } yield f"$a%x:$b%x:$c%x:$d%x:$e%x:$f%x:$g%x:$h%x"

  private val ipAddress: Gen[String] = Gen.oneOf(ipv4Address, ipv6Address)

  private val urlSchemeGen: Gen[String] = Gen.oneOf("http", "https")
  private val urlPrefixGen: Gen[String] = Gen.oneOf("", "www.")
  private val urlDomainGen: Gen[String] = strGen(7, Gen.alphaNumChar)
  private val urlTldGen: Gen[String] = Gen.oneOf(".com", ".net", ".co.uk", ".bg", ".ru", ".fr", ".tr")
  private val urlPathGen: Gen[String] = strGen(15, Gen.alphaNumChar)

  private val pageTitleGen: Gen[String] = Gen.chooseNum(1, 10).map(n => Random.shuffle(LoremIpsum.take(n)).mkString(" ").capitalize)

  private val webPageContextGen: Gen[SelfDescribingData[Json]] = for {
    id <- Gen.uuid
    data = Map("id" -> id.toString).asJson
    selfDesc = SelfDescribingData[Json](WebPageSchemaKey, data)
  } yield selfDesc

  private val geoLocationContextGen: Gen[SelfDescribingData[Json]] = for {
    lat <- Gen.chooseNum[Double](-90, 90)
    lon <- Gen.chooseNum[Double](-180, 180)
    data = Map("latitude" -> lat, "longitude" -> lon).asJson
    selfDesc = SelfDescribingData[Json](GeoLocationSchemaKey, data)
  } yield selfDesc

  private val perfTimingContextGen = for {
    ns <- Gen.listOfN[Int](27, Gen.chooseNum[Int](1, 100000))
    data = PerfTimingSchemaProps.zip(ns.padTo(27, 0)).toMap.asJson
    selfDesc = SelfDescribingData[Json](PerfTimingSchemaKey, data)
  } yield selfDesc

  private val eventContextGen: Gen[Contexts] = for {
    wp <- webPageContextGen
    gl <- geoLocationContextGen
    pt <- perfTimingContextGen
    c  <- ContextsGen.clientSessionGen
  } yield Contexts(List(wp, gl, pt, c))

  private val derivedContextGen: Gen[Contexts] =
    ContextsGen.contextsGen

  private val linkClickUnstructGen: Gen[UnstructEvent] = for {
    scheme <- urlSchemeGen
    prefix <- urlPrefixGen
    domain <- urlDomainGen
    tld <- urlTldGen
    host = s"$prefix$domain$tld"
    path <- urlPathGen
    targetUrl = s"$scheme://$host/$path"
    data = Map("targetUrl" -> targetUrl).asJson
    selfDesc = SelfDescribingData[Json](LinkClickSchemaKey, data)
  } yield UnstructEvent(Some(selfDesc))

  private def strGen(n: Int, gen: Gen[Char]): Gen[String] =
    Gen.chooseNum(1, n).flatMap(len => Gen.listOfN(len, gen).map(_.mkString))


  def pageViewGen(idPair: Duplicates.Pair): Gen[Event] =
    for {
    timestamps <- Timestamps.gen

    id = idPair.id
    fingerprint = idPair.hash
    collectorTstamp = timestamps.collector
    etlTstamp = timestamps.etl
    userIpAddress <- ipAddress
    domainUserId <- Gen.uuid
    domainSessionIdx <- Gen.chooseNum(1, 10000)
    networkUserId <- Gen.uuid
    pageUrlScheme <- urlSchemeGen
    pageUrlPrefix <- urlPrefixGen
    pageUrlDomain <- urlDomainGen
    pageUrlTld <- urlTldGen
    pageUrlHost = s"$pageUrlPrefix$pageUrlDomain$pageUrlTld"
    pageUrlPath <- urlPathGen
    pageUrl = s"$pageUrlScheme://$pageUrlHost/$pageUrlPath"
    pageTitle <- pageTitleGen
    refrUrlScheme <- urlSchemeGen
    refrUrlPrefix <- urlPrefixGen
    refrUrlTld <- urlTldGen
    refrUrlHost = s"$refrUrlPrefix$RefrUrlDomain$refrUrlTld"
    refrUrlPath <- urlPathGen
    contexts <- eventContextGen
    pageReferrer = s"$refrUrlScheme://$refrUrlHost/$refrUrlPath"
    derivedContexts <- derivedContextGen
    domainSessionId <- Gen.uuid
    derivedTstamp = timestamps.derived
  } yield emptyEvent(id, collectorTstamp, VCollector, VEtl).copy(
    app_id = Some(AppId),
    platform = Some(Platform),
    etl_tstamp = Some(etlTstamp),
    event = Some("page_view"),
    name_tracker = Some(NameTracker),
    v_tracker = Some(VTracker),
    user_id = Some(UserId),
    user_ipaddress = Some(userIpAddress),
    domain_userid = Some(domainUserId.toString),
    domain_sessionidx = Some(domainSessionIdx),
    network_userid = Some(networkUserId.toString),
    page_url = Some(pageUrl),
    page_title = Some(pageTitle),
    page_referrer = Some(pageReferrer),
    page_urlscheme = Some(pageUrlScheme),
    page_urlhost = Some(pageUrlHost),
    page_urlpath = Some(pageUrlPath),
    refr_urlscheme = Some(refrUrlScheme),
    refr_urlhost = Some(refrUrlHost),
    refr_urlpath = Some(refrUrlPath),
    refr_medium = Some(RefrMedium),
    refr_source = Some(RefrSource),
    mkt_medium = Some(MktMedium),
    mkt_source = Some(MktSource),
    mkt_campaign = Some(MktCampaign),
    contexts = contexts,
    useragent = Some(Useragent),
    derived_contexts = derivedContexts,
    domain_sessionid = Some(domainSessionId.toString),
    derived_tstamp = Some(derivedTstamp),
    event_vendor = Some("com.snowplowanalytics.snowplow"),
    event_name = Some("page_view"),
    event_format = Some(EventFormat),
    event_version = Some("1-0-0")
  )

  def linkClickGen(idPair: Duplicates.Pair): Gen[Event] =
    for {
      timestamps <- Timestamps.gen
      id          = idPair.id
      fingerprint = idPair.hash

      collectorTstamp = timestamps.collector
      etlTstamp = timestamps.etl
      userIpAddress <- ipAddress
      domainUserId <- Gen.uuid
      domainSessionIdx <- Gen.chooseNum(1, 10000)
      networkUserId <- Gen.uuid
      pageUrlScheme <- urlSchemeGen
      pageUrlPrefix <- urlPrefixGen
      pageUrlDomain <- urlDomainGen
      pageUrlTld <- urlTldGen
      pageUrlHost = s"$pageUrlPrefix$pageUrlDomain$pageUrlTld"
      pageUrlPath <- urlPathGen
      pageUrl = s"$pageUrlScheme://$pageUrlHost/$pageUrlPath"
      pageTitle <- pageTitleGen
      refrUrlScheme <- urlSchemeGen
      refrUrlPrefix <- urlPrefixGen
      refrUrlTld <- urlTldGen
      refrUrlHost = s"$refrUrlPrefix$RefrUrlDomain$refrUrlTld"
      refrUrlPath <- urlPathGen
      contexts <- eventContextGen
      unstruct <- linkClickUnstructGen
      pageReferrer = s"$refrUrlScheme://$refrUrlHost/$refrUrlPath"
      derivedContexts <- derivedContextGen
      domainSessionId <- Gen.uuid
      derivedTstamp = timestamps.derived
    } yield emptyEvent(id, collectorTstamp, VCollector, VEtl).copy(
      app_id = Some(AppId),
      platform = Some(Platform),
      etl_tstamp = Some(etlTstamp),
      event = Some("unstruct"),
      name_tracker = Some(NameTracker),
      v_tracker = Some(VTracker),
      user_id = Some(UserId),
      user_ipaddress = Some(userIpAddress),
      domain_userid = Some(domainUserId.toString),
      domain_sessionidx = Some(domainSessionIdx),
      network_userid = Some(networkUserId.toString),
      page_url = Some(pageUrl),
      page_title = Some(pageTitle),
      page_referrer = Some(pageReferrer),
      page_urlscheme = Some(pageUrlScheme),
      page_urlhost = Some(pageUrlHost),
      page_urlpath = Some(pageUrlPath),
      refr_urlscheme = Some(refrUrlScheme),
      refr_urlhost = Some(refrUrlHost),
      refr_urlpath = Some(refrUrlPath),
      refr_medium = Some(RefrMedium),
      refr_source = Some(RefrSource),
      mkt_medium = Some(MktMedium),
      mkt_source = Some(MktSource),
      mkt_campaign = Some(MktCampaign),
      contexts = contexts,
      unstruct_event = unstruct,
      useragent = Some(Useragent),
      derived_contexts = derivedContexts,
      domain_sessionid = Some(domainSessionId.toString),
      derived_tstamp = Some(derivedTstamp),
      event_vendor = Some("com.snowplowanalytics.snowplow"),
      event_name = Some("link_click"),
      event_format = Some(EventFormat),
      event_version = Some("1-0-1"),
      event_fingerprint = Some(fingerprint)
    )


  def generateEvent[F[_]: Sync](idPair: Duplicates.Pair) =
    Duplicates.runGen[F, Event](Gen.oneOf(pageViewGen(idPair), linkClickGen(idPair)))

  def eventStream[F[_]: Sync](config: Config): Stream[F, Event] =
    Stream.resource(Duplicates.pregeneratePairs[F](config.duplicates.totalDupes)).flatMap { state =>
      Stream.repeatEval(Duplicates.generatePair[F](state, config.duplicates, config.total).flatMap(generateEvent[F]))
    }
    
}

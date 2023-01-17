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
package com.snowplowanalytics.snowplow.eventgen.protocol

import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._
import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer, SelfDescribingData}
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{Contexts, UnstructEvent}
import com.snowplowanalytics.snowplow.eventgen.primitives._
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json, JsonObject}
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen
import java.util.concurrent.atomic.AtomicLong

object Context {

  val changeFormGenCount = new AtomicLong()
  val changeFormGen =
    for {
      _         <- Gen.delay(Gen.const[Long](changeFormGenCount.incrementAndGet()))
      formId    <- strGen(32, Gen.alphaNumChar).withKey("formId")
      elementId <- strGen(32, Gen.alphaNumChar).withKey("elementId")
      nodeName  <- Gen.oneOf(List("INPUT", "TEXTAREA", "SELECT")).withKey("nodeName")
      `type` <- Gen
        .option(
          Gen.oneOf(
            List(
              "button",
              "checkbox",
              "color",
              "date",
              "datetime",
              "datetime-local",
              "email",
              "file",
              "hidden",
              "image",
              "month",
              "number",
              "password",
              "radio",
              "range",
              "reset",
              "search",
              "submit",
              "tel",
              "text",
              "time",
              "url",
              "week"
            )
          )
        )
        .withKeyOpt("type")
      value <- Gen.option(strGen(16, Gen.alphaNumChar)).withKeyNull("value")
    } yield SelfDescribingData(
      SchemaKey("com.snowplowanalytics.snowplow", "change_form", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(formId, elementId, nodeName, `type`, value))
    )

  val clientSessionGenCount = new AtomicLong()
  val clientSessionGen =
    for {
      _                 <- Gen.delay(Gen.const[Long](clientSessionGenCount.incrementAndGet()))
      userId            <- Gen.uuid.withKey("userId")
      sessionId         <- Gen.uuid.withKey("sessionId")
      sessionIndex      <- Gen.choose(0, 2147483647).withKey("sessionIndex")
      previousSessionId <- Gen.option(Gen.uuid).withKeyNull("previousSessionId")
      storageMechanism <- Gen
        .oneOf(List("SQLITE", "COOKIE_1", "COOKIE_3", "LOCAL_STORAGE", "FLASH_LSO"))
        .withKey("storageMechanism")
    } yield SelfDescribingData(
      SchemaKey("com.snowplowanalytics.snowplow", "client_session", "jsonschema", SchemaVer.Full(1, 0, 1)),
      asObject(List(userId, sessionId, sessionIndex, previousSessionId, storageMechanism))
    )

  val consentDocumentCount = new AtomicLong()
  val consentDocumentGen =
    for {
      _           <- Gen.delay(Gen.const[Long](consentDocumentCount.incrementAndGet()))
      id          <- strGen(36, Gen.alphaNumChar).withKey("id")
      version     <- strGen(36, Gen.alphaNumChar).withKey("version")
      name        <- Gen.option(strGen(60, Gen.alphaNumChar)).withKeyOpt("name")
      description <- Gen.option(strGen(1000, Gen.alphaNumChar)).withKeyOpt("description")
    } yield SelfDescribingData(
      SchemaKey("com.snowplowanalytics.snowplow", "consent_document", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(id, version, name, description))
    )

  val desktopContextCount = new AtomicLong()
  val desktopContextGen =
    for {
      _                    <- Gen.delay(Gen.const[Long](desktopContextCount.incrementAndGet()))
      osType               <- Gen.oneOf(List("Windows", "Linux", "macOS", "Solaris")).withKey("osType")
      osVersion            <- strGen(36, Gen.numChar).withKey("osVersion")
      osServicePack        <- Gen.option(strGen(48, Gen.alphaNumChar)).withKeyOpt("osServicePack")
      osIs64Bit            <- Gen.option(Gen.oneOf(List(true, false))).withKeyOpt("osIs64Bit")
      deviceManufacturer   <- Gen.option(strGen(36, Gen.numChar)).withKeyOpt("deviceManufacturer")
      deviceModel          <- Gen.option(strGen(36, Gen.numChar)).withKeyOpt("deviceModel")
      deviceProcessorCount <- Gen.option(Gen.choose(1, 32)).withKeyOpt("deviceProcessorCount")
    } yield SelfDescribingData(
      SchemaKey("com.snowplowanalytics.snowplow", "desktop_context", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(osType, osVersion, osIs64Bit, osServicePack, deviceManufacturer, deviceModel, deviceProcessorCount))
    )

  val httpCookieCount = new AtomicLong()
  val httpCookieGen =
    for {
      _     <- Gen.delay(Gen.const[Long](httpCookieCount.incrementAndGet()))
      name  <- strGen(32, Gen.alphaNumChar).withKey("name")
      value <- Gen.option(strGen(32, Gen.alphaNumChar)).withKeyNull("value")
    } yield SelfDescribingData(
      SchemaKey("org.ietf", "http_cookie", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(name, value))
    )

  val httpHeaderCount = new AtomicLong()
  val httpHeaderGen =
    for {
      _     <- Gen.delay(Gen.const[Long](httpHeaderCount.incrementAndGet()))
      name  <- strGen(16, Gen.alphaNumChar).withKey("name")
      value <- strGen(16, Gen.alphaNumChar).withKey("value")
    } yield SelfDescribingData(
      SchemaKey("org.ietf", "http_header", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(name, value))
    )

  val googleCookiesCount = new AtomicLong()
  val googleCookiesGen =
    for {
      _    <- Gen.delay(Gen.const[Long](googleCookiesCount.incrementAndGet()))
      utma <- Gen.option(strGen(12, Gen.alphaNumChar)).withKeyOpt("__utma")
      utmb <- Gen.option(strGen(12, Gen.alphaNumChar)).withKeyOpt("__utmb")
      utmc <- Gen.option(strGen(12, Gen.alphaNumChar)).withKeyOpt("__utmc")
      utmv <- Gen.option(strGen(12, Gen.alphaNumChar)).withKeyOpt("__utmv")
      utmz <- Gen.option(strGen(12, Gen.alphaNumChar)).withKeyOpt("__utmz")
      ga   <- Gen.option(strGen(12, Gen.alphaNumChar)).withKeyOpt("_ga")
    } yield SelfDescribingData(
      SchemaKey("com.google.analytics", "cookies", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(utma, utmb, utmc, utmv, utmz, ga))
    )

  val googlePrivateCount = new AtomicLong()
  val googlePrivateGen =
    for {
      _   <- Gen.delay(Gen.const[Long](googlePrivateCount.incrementAndGet()))
      v   <- Gen.option(strGen(16, Gen.alphaNumChar)).withKeyNull("v")
      s   <- Gen.option(Gen.choose(-128000, 1000000)).withKeyNull("s")
      u   <- Gen.option(strGen(256, Gen.alphaNumChar)).withKeyNull("u") // exceeds 256 on purpose
      gid <- Gen.option(strGen(16, Gen.alphaNumChar)).withKeyNull("gid")
      r   <- Gen.option(Gen.choose(-128000, 1000000)).withKeyNull("r")
    } yield SelfDescribingData(
      SchemaKey("com.google.analytics", "private", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(v, s, u, gid, r))
    )

  val optimizelyVisitorCount = new AtomicLong()
  val optimizelyVisitorGen =
    for {
      _              <- Gen.delay(Gen.const[Long](optimizelyVisitorCount.incrementAndGet()))
      browser        <- Gen.option(strGen(32, Gen.alphaNumChar)).withKeyNull("browser")
      browserVersion <- Gen.option(strGen(4, Gen.alphaNumChar)).withKeyNull("browserVersion")
      device         <- Gen.option(strGen(16, Gen.alphaNumChar)).withKeyNull("device")
      deviceType     <- Gen.option(strGen(16, Gen.alphaNumChar)).withKeyNull("deviceType")
      mobile         <- Gen.option(Gen.oneOf(true, false)).withKeyNull("mobile")
    } yield SelfDescribingData(
      SchemaKey("com.optimizely", "visitor", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(browser, browserVersion, device, deviceType, mobile))
    )

  val optimizelyStateCount = new AtomicLong()
  val optimizelyStateGen =
    for {
      _              <- Gen.delay(Gen.const[Long](optimizelyStateCount.incrementAndGet()))
      experimentId   <- Gen.option(strGen(32, Gen.alphaNumChar)).withKeyNull("experimentId")
      isActive       <- Gen.option(Gen.oneOf(true, false)).withKeyNull("isActive")
      variationIndex <- Gen.option(Gen.choose(-100, 32767)).withKeyNull("variationIndex")
      variationId    <- Gen.option(strGen(16, Gen.alphaNumChar)).withKeyNull("variationId")
      variationName  <- Gen.option(strGen(32, Gen.alphaNumChar)).withKeyNull("variationName")
    } yield SelfDescribingData(
      SchemaKey("com.optimizely", "state", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(experimentId, isActive, variationIndex, variationId, variationName))
    )

  val optimizelyVariationCount = new AtomicLong()
  val optimizelyVariationGen =
    for {
      _    <- Gen.delay(Gen.const[Long](optimizelyVariationCount.incrementAndGet()))
      id   <- Gen.option(strGen(32, Gen.alphaNumChar)).withKeyNull("id")
      name <- Gen.option(strGen(32, Gen.alphaNumChar)).withKeyNull("name")
      code <- Gen.option(strGen(32, Gen.alphaNumChar)).withKeyNull("code")
    } yield SelfDescribingData(
      SchemaKey("com.optimizely", "variation", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(id, name, code))
    )

  val optimizelySummaryCount = new AtomicLong()
  val optimizelySummaryGen =
    for {
      _             <- Gen.delay(Gen.const[Long](optimizelySummaryCount.incrementAndGet()))
      experimentId  <- Gen.option(Gen.choose(-10, 100000)).withKeyNull("experimentId")
      variationName <- Gen.option(strGen(32, Gen.alphaNumChar)).withKeyNull("variationName")
      variation     <- Gen.option(Gen.choose(-10, 100000)).withKeyOpt("variation")
      visitorId     <- Gen.option(Gen.option(strGen(32, Gen.alphaNumChar))).withKeyOpt("visitorId")
    } yield SelfDescribingData(
      SchemaKey("com.optimizely.optimizelyx", "summary", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(experimentId, variationName, variation, visitorId))
    )

  val sessionContextCount = new AtomicLong()
  val sessionContextGen =
    for {
      _  <- Gen.delay(Gen.const[Long](sessionContextCount.incrementAndGet()))
      id <- strGen(32, Gen.alphaNumChar).withKey("id")
    } yield SelfDescribingData(
      SchemaKey("com.mparticle.snowplow", "session_context", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(id))
    )

  val consentWithdrawnCount = new AtomicLong()
  val consentWithdrawnGen =
    for {
      _   <- Gen.delay(Gen.const[Long](consentWithdrawnCount.incrementAndGet()))
      all <- Gen.oneOf(true, false).withKey("all")
    } yield SelfDescribingData(
      SchemaKey("com.snowplowanalytics.snowplow", "consent_withdrawn", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(all))
    )

  val segmentScreenCount = new AtomicLong()
  val segmentScreenGen =
    for {
      _    <- Gen.delay(Gen.const[Long](segmentScreenCount.incrementAndGet()))
      name <- strGen(32, Gen.alphaNumChar).withKey("name")
    } yield SelfDescribingData(
      SchemaKey("com.segment", "screen", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(name))
    )

  val pushRegistrationCount = new AtomicLong()
  val pushRegistrationGen =
    for {
      _                 <- Gen.delay(Gen.const[Long](pushRegistrationCount.incrementAndGet()))
      name              <- strGen(32, Gen.alphaNumChar).withKey("name")
      registrationToken <- Gen.option(strGen(32, Gen.alphaNumChar)).withKeyOpt("registrationToken")
    } yield SelfDescribingData(
      SchemaKey("com.mparticle.snowplow", "pushregistration_event", "jsonschema", SchemaVer.Full(1, 0, 0)),
      asObject(List(name, registrationToken))
    )

  private val UaParserSchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow", "ua_parser_context", "jsonschema", SchemaVer.Full(1, 0, 0))
  private val useragentFamilyGen: Gen[String] = Gen.oneOf("Chrome", "Firefox", "Safari")
  private val osFamilyGen: Gen[String]        = Gen.oneOf("Linux", "Windows", "Mac OS X")
  private val deviceFamilyGen: Gen[String]    = Gen.oneOf("Mac", "iPhone", "Generic Feature Phone")

  var uaParserContextCount = new AtomicLong()

  val uaParserContextGen: Gen[SelfDescribingData[Json]] = for {
    _        <- Gen.delay(Gen.const[Long](uaParserContextCount.incrementAndGet()))
    uaFamily <- useragentFamilyGen
    uaMaj    <- Gen.chooseNum[Int](0, 10)
    uaMin    <- Gen.chooseNum[Int](0, 10)
    osFamily <- osFamilyGen
    dFamily  <- deviceFamilyGen
    data = Map(
      "useragentFamily" -> uaFamily,
      "useragentMajor"  -> uaMaj.toString,
      "useragentMinor"  -> uaMin.toString,
      "osFamily"        -> osFamily,
      "deviceFamily"    -> dFamily
    ).asJson
  } yield SelfDescribingData[Json](UaParserSchemaKey, data)

  def contextsGen: Gen[Contexts] =
    for {
      clientSession       <- Gen.option(clientSessionGen)
      consentDocument     <- consentDocumentGen
      desktopContext      <- desktopContextGen
      uaParserContext     <- uaParserContextGen // derived context actually
      httpCookie          <- httpCookieGen
      httpHeader          <- httpHeaderGen
      googleCookies       <- googleCookiesGen
      googlePrivate       <- googlePrivateGen
      optimizelyVisitor   <- optimizelyVisitorGen
      optimizelyState     <- optimizelyStateGen
      optimizelyVariation <- optimizelyVariationGen
      optimizelySummary   <- optimizelySummaryGen
      sessionContext      <- sessionContextGen
      consentWithdrawn    <- consentWithdrawnGen
      segmentScreen       <- segmentScreenGen
      pushRegistration    <- pushRegistrationGen
    } yield Contexts(
      pushRegistration :: segmentScreen :: consentWithdrawn :: sessionContext :: optimizelySummary :: optimizelyVariation :: optimizelyState :: optimizelyVisitor :: googlePrivate :: googleCookies :: httpCookie :: httpHeader :: uaParserContext :: desktopContext :: consentDocument :: clientSession.toList
    )

  final case class ContextsWrapper(contexts: Contexts) extends Protocol {
    override def toProto: List[BasicNameValuePair] = asKV("cx", Some(base64Encode(contexts.asJson)))
  }

  object ContextsWrapper {
    val gen: Gen[ContextsWrapper] = contextsGen.map(ContextsWrapper.apply)

    val genOps: Gen[Option[ContextsWrapper]] = Gen.option(gen)
  }

  def singeContextGen: Gen[UnstructEvent] =
    Gen.oneOf(clientSessionGen, desktopContextGen).map(d => UnstructEvent(Some(d)))

  def toJson(unstructEvent: UnstructEvent): Json =
    SelfDescribingData(
      SchemaKey("com.snowplowanalytics.snowplow", "unstruct_event", "jsonschema", SchemaVer.Full(1, 0, 0)),
      unstructEvent.asJson
    ).asJson

  def toJson(contexts: Contexts): Json =
    SelfDescribingData(
      SchemaKey("com.snowplowanalytics.snowplow", "payload_data", "jsonschema", SchemaVer.Full(1, 0, 4)),
      contexts.data.asJson
    ).asJson

  // Helpers to control null/absence

  def asObject(fields: List[Option[(String, Json)]]): Json =
    JsonObject.fromIterable(fields.collect { case Some(field) => field }).asJson

  implicit class GenOps[A](gen: Gen[A]) {
    def withKey[B](name: String)(implicit enc: Encoder[A]): Gen[Option[(String, Json)]] =
      gen.map { a =>
        Some(name -> a.asJson)
      }
  }

  implicit class GenOptOps[A](gen: Gen[Option[A]]) {
    def withKeyOpt(name: String)(implicit enc: Encoder[A]): Gen[Option[(String, Json)]] =
      gen.map {
        case Some(a) => Some(name -> a.asJson)
        case None    => None
      }

    def withKeyNull(name: String)(implicit enc: Encoder[A]): Gen[Option[(String, Json)]] =
      gen.map {
        case Some(a) => Some(name -> a.asJson)
        case None    => Some(name -> Json.Null)
      }
  }
}

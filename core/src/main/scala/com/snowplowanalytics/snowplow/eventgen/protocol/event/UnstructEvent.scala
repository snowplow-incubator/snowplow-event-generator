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

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer, SelfDescribingData}
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{UnstructEvent => SdkUnstructEvent }
import com.snowplowanalytics.snowplow.eventgen.primitives.Url
import io.circe.Json
import io.circe.syntax._


object UnstructEvent {
  val LinkClickSchemaKey: SchemaKey = SchemaKey("com.snowplowanalytics.snowplow", "link_click", "jsonschema", SchemaVer.Full(1, 0, 1))

  trait UnstructEventData {
    def toUnstructEvent: SdkUnstructEvent
  }
  // TODO: refactor following functions
  //
  //  private def changeFormGen =
  //    for {
  //      formId <- strGen(32, Gen.alphaNumChar).withKey("formId")
  //      elementId <- strGen(32, Gen.alphaNumChar).withKey("elementId")
  //      nodeName <- Gen.oneOf(List("INPUT", "TEXTAREA", "SELECT")).withKey("nodeName")
  //      `type` <- Gen.option(Gen.oneOf(List("button", "checkbox", "color", "date", "datetime", "datetime-local", "email", "file", "hidden", "image", "month", "number", "password", "radio", "range", "reset", "search", "submit", "tel", "text", "time", "url", "week"))).withKeyOpt("type")
  //      value <- Gen.option(strGen(16, Gen.alphaNumChar)).withKeyNull("value")
  //    } yield SelfDescribingData(
  //      SchemaKey("com.snowplowanalytics.snowplow", "change_form", "jsonschema", SchemaVer.Full(1, 0, 0)),
  //      asObject(List(formId, elementId, nodeName, `type`, value))
  //    )
  //
  //  private def clientSessionGen =
  //    for {
  //      userId <- Gen.uuid.withKey("userId")
  //      sessionId <- Gen.uuid.withKey("sessionId")
  //      sessionIndex <- Gen.choose(0, 2147483647).withKey("sessionIndex")
  //      previousSessionId <- Gen.option(Gen.uuid).withKeyNull("previousSessionId")
  //      storageMechanism <- Gen.oneOf(List("SQLITE", "COOKIE_1", "COOKIE_3", "LOCAL_STORAGE", "FLASH_LSO")).withKey("storageMechanism")
  //    } yield SelfDescribingData(
  //      SchemaKey("com.snowplowanalytics.snowplow", "client_session", "jsonschema", SchemaVer.Full(1, 0, 1)),
  //      asObject(List(userId, sessionId, sessionIndex, previousSessionId, storageMechanism))

  val genLink = Url.gen.map(LinkClick.apply)

  final case class LinkClick(url: Url) extends UnstructEventData {
    def schema: SchemaKey = UnstructEvent.LinkClickSchemaKey

    def data: Json = Map("targetUrl" -> url.toString).asJson

    def toUnstructEvent: SdkUnstructEvent = SdkUnstructEvent(Some(SelfDescribingData(schema, data))
    )
  }
}



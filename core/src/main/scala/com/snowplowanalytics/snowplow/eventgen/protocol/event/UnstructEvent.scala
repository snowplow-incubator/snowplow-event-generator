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
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.{UnstructEvent => SdkUnstructEvent}
import com.snowplowanalytics.snowplow.eventgen.primitives.{Url, strGen}
import io.circe.Json
import io.circe.syntax._
import org.scalacheck.Gen

object UnstructEvent {
  trait UnstructEventData {
    def schema: SchemaKey
    def data: Json

    def toUnstructEvent: SdkUnstructEvent = SdkUnstructEvent(Some(SelfDescribingData(schema, data)))
  }

  object schemas {
    val LinkClick: SchemaKey =
      SchemaKey("com.snowplowanalytics.snowplow", "link_click", "jsonschema", SchemaVer.Full(1, 0, 1))
    val ChangeForm: SchemaKey =
      SchemaKey("com.snowplowanalytics.snowplow", "change_form", "jsonschema", SchemaVer.Full(1, 0, 0))
  }

  final case class LinkClick(url: Url) extends UnstructEventData {
    def schema: SchemaKey = UnstructEvent.schemas.LinkClick
    def data: Json        = Map("targetUrl" -> url.toString).asJson
  }

  object LinkClick {
    val gen: Gen[LinkClick] = Url.gen.map(LinkClick.apply)
  }

  final case class ChangeForm(
    formId: String,
    elementId: String,
    nodeName: String,
    `type`: Option[String],
    value: Option[String]
  ) extends UnstructEventData {
    def schema: SchemaKey = UnstructEvent.schemas.ChangeForm
    def data: Json =
      Map(
        "formId"    -> Some(formId),
        "elementId" -> Some(elementId),
        "nodeName"  -> Some(nodeName),
        "type"      -> Some(`type`.getOrElse("")), // we do want an empty String here,
        "value"     -> value
      ).filterNot { case (_, v) => v.contains("") }.asJson
  }

  object ChangeForm {
    val gen: Gen[ChangeForm] = for {
      formId    <- strGen(32, Gen.alphaNumChar)
      elementId <- strGen(32, Gen.alphaNumChar)
      nodeName  <- Gen.oneOf(List("INPUT", "TEXTAREA", "SELECT"))
      `type` <- Gen.option(
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
      value <- Gen.option(strGen(16, Gen.alphaNumChar))
    } yield ChangeForm(formId, elementId, nodeName, `type`, value)
  }
}

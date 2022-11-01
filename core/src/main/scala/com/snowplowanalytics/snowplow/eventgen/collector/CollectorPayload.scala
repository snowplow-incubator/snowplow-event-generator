/*
 * Copyright (c) 2012-2022 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.eventgen.collector

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer, SelfDescribingData}
import com.snowplowanalytics.snowplow.CollectorPayload.thrift.model1.{CollectorPayload => CollectorPayload1}
import com.snowplowanalytics.snowplow.eventgen.protocol._
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._
import com.snowplowanalytics.snowplow.eventgen.protocol.common.PayloadDataSchema
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.thrift.TSerializer
import org.scalacheck.Gen
import io.circe.Json
import io.circe.syntax.EncoderOps

import scala.jdk.CollectionConverters._
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

final case class CollectorPayload(
  api: Api,
  payload: List[Body],
  source: Source,
  context: CollectorContext
) {
  private case class PayloadParts(
    querystring: List[NameValuePair],
    bodyJson: Option[Json],
    contentType: Option[String],
    timestamp: Long
  ) {
    def body: Option[String] = bodyJson.map(_.noSpaces)
  }

  private def encodeValue(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString)

  private[this] lazy val parts: PayloadParts = {
    val timestamp: Long                  = context.timestamp.toEpochMilli
    var querystring: List[NameValuePair] = List.empty[NameValuePair]
    var contentType: Option[String]      = None
    var bodyJson: Option[Json]           = None

    if (api.version == "tp1") {
      querystring = payload.head.toProto.map(kv => new BasicNameValuePair(kv.getName, encodeValue(kv.getValue)))
    } else {
      contentType = Some("application/json")
      bodyJson = Some(
        SelfDescribingData(
          PayloadDataSchema.Default,
          payload.map(body => body.toPayloadElement).asJson
        ).asJson
      )
    }
    PayloadParts(querystring, bodyJson, contentType, timestamp)
  }

  def toThrift: CollectorPayload1 =
    // Timestamp must be always set, otherwise long will fallback it to 1970-01-01
    new CollectorPayload1(
      CollectorPayload.IgluUri.toSchemaUri,
      context.ipAddress.map(_.toString).orNull,
      parts.timestamp,
      source.encoding,
      source.name
    ).setHostname(source.hostname.orNull)
      .setQuerystring(
        (new URIBuilder).setPath(api.toString).setParameters(parts.querystring.asJava).build().getQuery
      )
      .setRefererUri(context.refererUri.map(_.toString).orNull)
      .setContentType(parts.contentType.orNull)
      .setUserAgent(context.useragent.orNull)
      .setBody(parts.body.orNull)
      .setNetworkUserId(context.userId.map(_.toString).orNull)
      .setHeaders(context.headers.toList.asJava)
      .setPath(api.toString);

  override def toString: String =
    s"""
       #################################### NEW EVENT ####################################
       ############  ############  ############ QueryString ############  ############  ##########
       ${(new URIBuilder).setPath(api.toString).setParameters(parts.querystring.asJava).build().getQuery}

       ${parts.querystring.foldLeft("\n")((acc, nv) => acc + nv.getName + "  :  " + nv.getValue + "\n")}
       ############  ############  ############  BODY ############  ############  ############
       ${parts.bodyJson.getOrElse(None.asJson).spaces2SortKeys}
       ############  ############  ############  API ############  ############  ############
       ${api.toString}
       ############  ############  ############  TS ############  ############  ############
       ${parts.timestamp}
       ############  ############  ############  CTX ############  ############  ############
       ${context.toString}
       ############  ############  ############  SRC ############  ############  ############
       ${source.toString}
       #########################################################################################
       """.stripMargin

  def toRaw: Array[Byte] =
    CollectorPayload.serializer.serialize(toThrift)
}

object CollectorPayload {
  def genDup(
    natProb: Float,
    synProb: Float,
    natTotal: Int,
    synTotal: Int,
    eventPerPayloadMin: Int,
    eventPerPayloadMax: Int,
    now: Instant
  ): Gen[CollectorPayload] =
    genWithBody(eventPerPayloadMin, eventPerPayloadMax, Body.genDup(natProb, synProb, natTotal, synTotal, now), now)

  private def genWithBody(eventPerPayloadMin: Int, eventPerPayloadMax: Int, bodyGen: Gen[Body], now: Instant) =
    for {
      n       <- Gen.chooseNum(eventPerPayloadMin, eventPerPayloadMax)
      api     <- Api.genApi(n)
      src     <- Source.gen
      cc      <- CollectorContext.gen(now)
      payload <- Gen.listOfN(n, bodyGen)
    } yield CollectorPayload(api, payload, src, cc)

  def gen(eventPerPayloadMin: Int, eventPerPayloadMax: Int, now: Instant): Gen[CollectorPayload] =
    genWithBody(eventPerPayloadMin, eventPerPayloadMax, Body.gen(now), now)

  val IgluUri: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow", "CollectorPayload", "thrift", SchemaVer.Full(1, 0, 0))

  lazy val serializer = new TSerializer()
}

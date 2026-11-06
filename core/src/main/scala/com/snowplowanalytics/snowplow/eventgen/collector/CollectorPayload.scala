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
import com.snowplowanalytics.snowplow.eventgen.protocol._
import com.snowplowanalytics.snowplow.eventgen.GenConfig
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._
import com.snowplowanalytics.snowplow.eventgen.protocol.common.PayloadDataSchema
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicNameValuePair
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

  private[collector] def thriftFields: CollectorPayloadThrift =
    CollectorPayloadThrift(
      schema = CollectorPayload.IgluUri.toSchemaUri,
      ipAddress = context.ipAddress.map(_.toString),
      timestamp = parts.timestamp,
      encoding = source.encoding,
      collector = source.name,
      userAgent = context.useragent,
      refererUri = context.refererUri.map(_.toString),
      path = Some(api.toString),
      querystring = Option(
        (new URIBuilder).setPath(api.toString).setParameters(parts.querystring.asJava).build().getQuery
      ),
      body = parts.body,
      headers = Some(context.headers.toList),
      contentType = parts.contentType,
      hostname = source.hostname,
      networkUserId = context.userId.map(_.toString)
    )

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
    thriftFields.toBytes
}

object CollectorPayload {

  def gen(
    eventsPerPayload: GenConfig.EventsPerPayload,
    time: Instant,
    frequencies: GenConfig.EventsFrequencies,
    contexts: GenConfig.ContextsPerEvent,
    identitySource: GenConfig.IdentitySource,
    duplicates: Option[GenConfig.Duplicates],
    appIds: List[String]
  ): Gen[CollectorPayload] = {
    // ProfileGraph identity source specifies its own appId, overriding the configured list
    val effectiveAppIds = identitySource match {
      case GenConfig.IdentitySource.ProfileGraph(profileAppId, _) => List(profileAppId)
      case _                                                      => appIds
    }
    genWithBody(
      eventsPerPayload,
      Body.gen(time, frequencies, contexts, identitySource, duplicates, effectiveAppIds),
      time
    )
  }

  private def genWithBody(eventsPerPayload: GenConfig.EventsPerPayload, bodyGen: Gen[Body], time: Instant) =
    for {
      n       <- Gen.chooseNum(eventsPerPayload.min, eventsPerPayload.max)
      api     <- Api.gen
      src     <- Source.gen
      cc      <- CollectorContext.gen(time)
      payload <- Gen.listOfN(n, bodyGen)
    } yield CollectorPayload(api, payload, src, cc)

  val IgluUri: SchemaKey =
    SchemaKey("com.snowplowanalytics.snowplow", "CollectorPayload", "thrift", SchemaVer.Full(1, 0, 0))
}

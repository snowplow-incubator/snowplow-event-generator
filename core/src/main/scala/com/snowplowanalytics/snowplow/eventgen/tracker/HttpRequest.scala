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
package com.snowplowanalytics.snowplow.eventgen.tracker

import com.snowplowanalytics.snowplow.eventgen.collector.Api
import com.snowplowanalytics.snowplow.eventgen.collector.Api._
import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest.Method
import org.scalacheck.Gen
import com.snowplowanalytics.snowplow.eventgen.GenConfig

import java.time.Instant

final case class HttpRequest(
  method: Method,
  headers: Map[String, String],
  qs: Option[HttpRequestQuerystring],
  body: Option[HttpRequestBody]
)

object HttpRequest {
  sealed trait Method {
    val path: Api
  }

  object Method {
    final case class Post(path: Api) extends Method
    final case class Get(path: Api) extends Method
    final case class Head(path: Api) extends Method

    def gen(freq: GenConfig.Events.Http.MethodFrequencies, validEventsOnly: Boolean): Gen[Method] =
      Gen.frequency(
        (freq.post, genPost(validEventsOnly)),
        (freq.get, genGet(validEventsOnly)),
        (freq.head, genHead(validEventsOnly))
      )

    private def genPost(validEventsOnly: Boolean): Gen[Method.Post] =
      if (validEventsOnly) genApi(200).map(Method.Post)
      else Gen.oneOf(genApi(0), genApi(200)).map(Method.Post)

    private def genGet(validEventsOnly: Boolean): Gen[Method.Get] =
      if (validEventsOnly) Gen.oneOf(fixedApis, genApi(1)).map(Method.Get)
      else Gen.oneOf(fixedApis, genApi(0), genApi(1)).map(Method.Get)

    private def genHead(validEventsOnly: Boolean): Gen[Method.Head] =
      if (validEventsOnly) Gen.oneOf(fixedApis, genApi(1)).map(Method.Head)
      else Gen.oneOf(fixedApis, genApi(0), genApi(1)).map(Method.Head)
  }

  def gen(
    eventsPerPayload: GenConfig.EventsPerPayload,
    time: Instant,
    frequencies: GenConfig.EventsFrequencies,
    contexts: GenConfig.ContextsPerEvent,
    methodFrequencies: Option[GenConfig.Events.Http.MethodFrequencies],
    identitySource: GenConfig.IdentitySource,
    duplicates: Option[GenConfig.Duplicates],
    validEventsOnly: Boolean
  ): Gen[HttpRequest] = {
    val methodFreq = methodFrequencies.getOrElse(new GenConfig.Events.Http.MethodFrequencies(1, 1, 1))
    genWithParts(
      HttpRequestQuerystring.gen(time, frequencies, contexts, identitySource),
      HttpRequestBody.gen(eventsPerPayload, time, frequencies, contexts, identitySource, duplicates),
      methodFreq,
      validEventsOnly
    )
  }

  private def genWithParts(
    qsGen: Gen[HttpRequestQuerystring],
    bodyGen: Gen[HttpRequestBody],
    methodFreq: GenConfig.Events.Http.MethodFrequencies,
    validEventsOnly: Boolean
  ) =
    for {
      method <- Method.gen(methodFreq, validEventsOnly)
      qs     <- Gen.option(qsGen)
      body <- method match {
        case Method.Head(_)                    => Gen.const(None)      // HEAD requests can't have a message body
        case Method.Post(_) if validEventsOnly => bodyGen.map(Some(_)) // POST to tp2 requires a body
        case _                                 => Gen.option(bodyGen)
      }
      generatedHs <- HttpRequestHeaders.genDefaultHeaders
      raw = HttpRequestHeaders.rawReqUriHeader(qs)
    } yield HttpRequest(method, generatedHs ++ raw, qs, body)
}

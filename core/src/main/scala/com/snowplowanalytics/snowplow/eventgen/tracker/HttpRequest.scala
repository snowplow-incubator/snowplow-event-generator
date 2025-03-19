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
  body: Option[String]
)

object HttpRequest {
  sealed trait Method {
    val path: Api
  }

  object Method {
    final case class Post(path: Api) extends Method
    final case class Get(path: Api) extends Method
    final case class Head(path: Api) extends Method

    def gen(freq: GenConfig.Events.Http.MethodFrequencies): Gen[Method] =
      Gen.frequency(
        (freq.post, genPost),
        (freq.get, genGet),
        (freq.head, genHead)
      )

    private def genPost: Gen[Method.Post] = Gen.oneOf(genApi(0), genApi(200)).map(Method.Post)
    private def genGet: Gen[Method.Get]   = Gen.oneOf(fixedApis, genApi(0), genApi(1)).map(Method.Get)
    private def genHead: Gen[Method.Head] = Gen.oneOf(fixedApis, genApi(0), genApi(1)).map(Method.Head)
  }

  def gen(
    eventsPerPayload: GenConfig.EventsPerPayload,
    time: Instant,
    frequencies: GenConfig.EventsFrequencies,
    contexts: GenConfig.ContextsPerEvent,
    methodFrequencies: Option[GenConfig.Events.Http.MethodFrequencies]
  ): Gen[HttpRequest] = {
    // MethodFrequencies is an option here, at the entrypoint in order not to force a breaking change where this is a lib.
    // If it's not provided, we give equal distribution to each to achieve behaviour parity
    // From here in it's not an option, just to make the code a bit cleaner
    val methodFreq = methodFrequencies.getOrElse(new GenConfig.Events.Http.MethodFrequencies(1, 1, 1))
    genWithParts(
      HttpRequestQuerystring.gen(time, frequencies, contexts),
      HttpRequestBody.gen(eventsPerPayload, time, frequencies, contexts),
      methodFreq
    )
  }

  def genDup(
    duplicates: GenConfig.Duplicates,
    eventsPerPayload: GenConfig.EventsPerPayload,
    time: Instant,
    frequencies: GenConfig.EventsFrequencies,
    contexts: GenConfig.ContextsPerEvent,
    methodFrequencies: Option[GenConfig.Events.Http.MethodFrequencies]
  ): Gen[HttpRequest] = {
    // MethodFrequencies is an option here, at the entrypoint in order not to force a breaking change where this is a lib.
    // If it's not provided, we give equal distribution to each to achieve behaviour parity
    // From here in it's not an option, just to make the code a bit cleaner
    val methodFreq = methodFrequencies.getOrElse(new GenConfig.Events.Http.MethodFrequencies(1, 1, 1))
    genWithParts(
      // qs doesn't do duplicates?
      HttpRequestQuerystring.gen(time, frequencies, contexts),
      HttpRequestBody.genDup(
        duplicates,
        eventsPerPayload,
        time,
        frequencies,
        contexts
      ),
      methodFreq
    )
  }

  private def genWithParts(
    qsGen: Gen[HttpRequestQuerystring],
    bodyGen: Gen[HttpRequestBody],
    methodFreq: GenConfig.Events.Http.MethodFrequencies
  ) =
    for {
      method <- Method.gen(methodFreq)
      qs     <- Gen.option(qsGen)
      body <- method match {
        case Method.Head(_) => Gen.const(None) // HEAD requests can't have a message body
        case _              => Gen.option(bodyGen)
      }
      generatedHs <- HttpRequestHeaders.genDefaultHeaders
      raw = HttpRequestHeaders.rawReqUriHeader(qs)
    } yield HttpRequest(method, generatedHs ++ raw, qs, body.map(_.toString))
}

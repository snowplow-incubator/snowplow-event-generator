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

    def gen: Gen[Method] = Gen.oneOf(genPost, genGet, genHead)

    private def genPost: Gen[Method.Post] = Gen.oneOf(genApi(0), genApi(200)).map(Method.Post)
    private def genGet: Gen[Method.Get]   = Gen.oneOf(fixedApis, genApi(0), genApi(1)).map(Method.Get)
    private def genHead: Gen[Method.Head] = Gen.oneOf(fixedApis, genApi(0), genApi(1)).map(Method.Head)
  }

  def gen(eventPerPayloadMin: Int, eventPerPayloadMax: Int, now: Instant): Gen[HttpRequest] =
    genWithParts(HttpRequestQuerystring.gen(now), HttpRequestBody.gen(eventPerPayloadMin, eventPerPayloadMax, now))

  private def genWithParts(qsGen: Gen[HttpRequestQuerystring], bodyGen: Gen[HttpRequestBody]) =
    for {
      method      <- Method.gen
      qs          <- Gen.option(qsGen)
      body        <- Gen.option(bodyGen)
      generatedHs <- HttpRequestHeaders.genDefaultHeaders
      raw = HttpRequestHeaders.rawReqUriHeader(qs)
    } yield HttpRequest(method, generatedHs ++ raw, qs, body)
}

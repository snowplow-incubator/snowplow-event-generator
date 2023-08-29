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
import com.snowplowanalytics.snowplow.eventgen.protocol.event.EventFrequencies

import java.time.Instant
import com.snowplowanalytics.snowplow.eventgen.protocol.Body

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

  case class MethodFrequencies(
    get: Int,
    post: Int,
    head: Int
  )

  case class PathFrequencies(
    i: Int,
    ice: Int,
    tp1: Int,
    tp2: Int,
    random: Int,
    providedPath: Option[ProvidedPathFrequency]
  )

  case class ProvidedPathFrequency(
    vendor: String,
    version: String,
    frequency: Int
  )

  object Method {
    final case class Post(path: Api) extends Method
    final case class Get(path: Api) extends Method
    final case class Head(path: Api) extends Method

    def gen(methodFreq: MethodFrequencies, pathFreq: PathFrequencies): Gen[Method] = {
      Gen.frequency(
        (methodFreq.post, genPost(pathFreq)), 
        (methodFreq.get, genGet(pathFreq)),
        (methodFreq.head, genHead(pathFreq))
        )
          }

    private def genPost(pathFreq: PathFrequencies): Gen[Method.Post] = 
      Gen.frequency(
        pathFreq.providedPath match {
          case Some(provided) => (provided.frequency, Api(provided.vendor, provided.version))
          case None => (0, Api("", ""))
        },
        (pathFreq.random, genApi(0)), 
        (pathFreq.tp2, genApi(2)),
        ).map(Method.Post)

    private def genGet(pathFreq: PathFrequencies): Gen[Method.Get]   = 
      Gen.frequency(
          pathFreq.providedPath match {
            case Some(provided) => (provided.frequency, Api(provided.vendor, provided.version))
            case None => (0, Api("", ""))
          },
          (pathFreq.i, Api.GenI),         // /i 
          (pathFreq.ice, Api.GenIce),     // /ice.png 
          (pathFreq.random, genApi(0)),   // randomly generated path
          (pathFreq.tp1, genApi(1)),      // /com.snowplowanalytics.snowplow/tp1
          (pathFreq.tp2, genApi(2))       // /com.snowplowanalytics.snowplow/tp2
          ).map(Method.Get)

    private def genHead(pathFreq: PathFrequencies): Gen[Method.Head] = 
      Gen.frequency(
        pathFreq.providedPath match {
          case Some(provided) => (provided.frequency, Api(provided.vendor, provided.version))
          case None => (0, Api("", ""))
        },
        (pathFreq.i, Api.GenI), 
        (pathFreq.ice, Api.GenIce), 
        (pathFreq.random, genApi(0)), 
        (pathFreq.tp1, genApi(1)), 
        (pathFreq.tp2, genApi(2))
        ).map(Method.Head)
  }

  def gen(
    eventPerPayloadMin: Int,
    eventPerPayloadMax: Int,
    now: Instant,
    frequencies: EventFrequencies,
    methodFrequencies: Option[MethodFrequencies],
    pathFrequencies: Option[PathFrequencies]
  ): Gen[HttpRequest] = {
    // MethodFrequencies and pathFrequencies are options here, at the entrypoint in order not to force a breaking change where this is a lib.
    // If it's not provided, we give equal distribution to each to achieve behaviour parity
    // From here in it's not an option, just to make the code a bit cleaner
    val methodFreq = methodFrequencies.getOrElse(new MethodFrequencies(1, 1, 1))
    val pathFreq = pathFrequencies.getOrElse(new PathFrequencies(1,1,1,1,1, None))
    genWithParts(
      HttpRequestQuerystring.gen(now, frequencies),
      HttpRequestBody.gen(eventPerPayloadMin, eventPerPayloadMax, now, frequencies),
      methodFreq,
      pathFreq   
    )
  }

  def genDup(
    natProb: Float,
    synProb: Float,
    natTotal: Int,
    synTotal: Int,
    eventPerPayloadMin: Int,
    eventPerPayloadMax: Int,
    now: Instant,
    frequencies: EventFrequencies,
    methodFrequencies: Option[MethodFrequencies],
    pathFrequencies: Option[PathFrequencies]
  ): Gen[HttpRequest] = {
    // MethodFrequencies and pathFrequencies are options here, at the entrypoint in order not to force a breaking change where this is a lib.
    // If it's not provided, we give equal distribution to each to achieve behaviour parity
    // From here in it's not an option, just to make the code a bit cleaner
    val methodFreq = methodFrequencies.getOrElse(new MethodFrequencies(1, 1, 1))
    val pathFreq = pathFrequencies.getOrElse(new PathFrequencies(1,1,1,1,1, None))
    genWithParts(
      // qs doesn't do duplicates?
      HttpRequestQuerystring.gen(now, frequencies),
      HttpRequestBody
        .genDup(natProb, synProb, natTotal, synTotal, eventPerPayloadMin, eventPerPayloadMax, now, frequencies),
      methodFreq,
      pathFreq
    )
  }

  def genDupFromBody(
    body: Gen[Body],
    eventPerPayloadMin: Int,
    eventPerPayloadMax: Int,
    methodFrequencies: Option[MethodFrequencies],
    pathFrequencies: Option[PathFrequencies]
    ): Gen[HttpRequest]  = {
    val methodFreq = methodFrequencies.getOrElse(new MethodFrequencies(1, 1, 1))
    val pathFreq = pathFrequencies.getOrElse(new PathFrequencies(1,1,1,1,1, None))
   genWithParts(
      HttpRequestQuerystring.genWithBody(body),
      HttpRequestBody
        .genWithBody(eventPerPayloadMin, eventPerPayloadMax, body),
      methodFreq,
      pathFreq
    )
  }    

  private def genWithParts(qsGen: Gen[HttpRequestQuerystring], bodyGen: Gen[HttpRequestBody], methodFreq: MethodFrequencies, pathFreq: PathFrequencies) =
    for {
      method <- Method.gen(methodFreq, pathFreq)
      qs     <- Gen.option(qsGen)
      body <- method match {
        case Method.Head(_) => Gen.const(None) // HEAD requests can't have a message body
        case _              => Gen.option(bodyGen)
      }
      generatedHs <- HttpRequestHeaders.genDefaultHeaders
      raw = HttpRequestHeaders.rawReqUriHeader(qs)
    } yield HttpRequest(method, generatedHs ++ raw, qs, body)
}

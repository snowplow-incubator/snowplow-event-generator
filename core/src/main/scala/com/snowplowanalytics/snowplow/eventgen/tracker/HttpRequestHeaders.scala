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

import com.snowplowanalytics.snowplow.eventgen.primitives.Url.genOriginHost
import com.snowplowanalytics.snowplow.eventgen.primitives.{Url, genIp, genString, genUserAgent}
import org.scalacheck.Gen

object HttpRequestHeaders {
  val FixedHeaders = Map("Accept" -> "*/*", "Content-Type" -> "application/json; charset=UTF-8")

  // The host of the raw request URI is irrelevant, since this header is only used to extract the querystring from the request
  def rawReqUriHeader(qs: Option[HttpRequestQuerystring]) =
    Map("Raw-Request-URI" -> List(Some("http://doesntmatt.er/p1/p2"), qs.map(_.toString)).flatten.mkString("?"))

  def genDefaultHeaders: Gen[Map[String, String]] =
    for {
      ua        <- Gen.option(genUserAgent)
      ref       <- Url.genOptStr
      ipName    <- Gen.oneOf(Gen.const("X-Forwarded-For"), Gen.const("X-Real-Ip"), Gen.const("Remote-Address"))
      ip        <- genIpHeaderValue(ipName)
      xFwdProto <- Gen.option(Gen.oneOf(Gen.const("http"), Gen.const("https")))
      origin    <- Gen.option(genOrigin)
      cookie    <- Gen.option(genCookie)
      anonymous <- Gen.option("*")
      custom    <- genCustomHeaders
    } yield Map(
      "User-Agent"        -> getV(ua),
      "Referer"           -> getV(ref),
      ipName              -> ip,
      "X-Forwarded-Proto" -> getV(xFwdProto),
      "Origin"            -> getV(origin),
      "Cookie"            -> getV(cookie),
      "SP-Anonymous"      -> getV(anonymous)
    ).filterNot { case (_, v) => v == "" } ++ FixedHeaders ++ custom

  private def genIpHeaderValue(headerName: String): Gen[String] =
    if (headerName != "X-Forwarded-For") genIp else genIps

  private def genIps =
    for {
      n   <- Gen.chooseNum(1, 3)
      ips <- Gen.listOfN(n, genIp)
    } yield ips.mkString(",")

  private def genOrigin =
    for {
      n  <- Gen.chooseNum(1, 3)
      os <- Gen.listOfN(n, genOriginHost)
    } yield os.map(getV).filterNot(_ == "").mkString(",")

  private def genCookie =
    for {
      n    <- Gen.chooseNum(1, 5)
      ks   <- Gen.listOfN(n, Gen.stringOfN(3, Gen.alphaNumChar))
      vs   <- Gen.listOfN(n, Gen.stringOfN(12, Gen.alphaNumChar))
      uuid <- Gen.option(Gen.uuid)
    } yield (ks.zip(vs).map { t =>
      s"${t._1}=${t._2}"
    } ++ uuid.toList.map(c => s"_sp=${c.toString}")).mkString(";")

  private def genCustomHeaders: Gen[Map[String, String]] =
    for {
      n             <- Gen.chooseNum(0, 10, 3, 5)
      customHeaders <- Gen.listOfN(n, genPair)
    } yield customHeaders.toMap

  private def genPair: Gen[(String, String)] =
    for {
      k <- genString("", 8)
      v <- genString("", 32)
    } yield (k, v)

  private def getV(o: Option[String]) = o.getOrElse("")
}

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
package com.snowplowanalytics.snowplow.eventgen.collector

import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequestHeaders.genDefaultHeaders
import org.scalacheck.Gen

case class Headers(
                    ref: Option[String],
                    ua: Option[String],
                    cookie: Option[String],
                    custom: Map[String, String]
                  ) {
  def toList: List[String] = List(
    ref.map(u => s"Referer: $u"),
    ua.map(ua => s"User-Agent: $ua"),
    cookie.map(cookie => s"Cookie: $cookie")
  ).flatten ++ custom.toList.map { case (k, v) => s"$k: $v" }
}

object Headers {
  def gen: Gen[Headers] = for {
    default <- genDefaultHeaders
    ref = default.get("Referer")
    ua = default.get("User-Agent")
    cookie = default.get("Cookie")
    custom = default.filterNot { case (k, _) => List("Referer", "User-Agent", "Cookie").contains(k)}
  } yield Headers(ref, ua, cookie, custom)
}

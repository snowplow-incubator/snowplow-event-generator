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
package com.snowplowanalytics.snowplow.eventgen.protocol

import org.apache.http.message.BasicNameValuePair

import java.time.Instant

trait Protocol {
  def toProto: List[BasicNameValuePair]

  def deps: List[Protocol] = List.empty[Protocol]

  private def mkString[T](v: T): String = {
    v match {
      case v: Boolean => if (v) "1" else "0"
      case v: Instant => v.toEpochMilli.toString
      case _ => v.toString
    }
  }

  def asKV[V](k: String, ov: Option[V]): List[BasicNameValuePair] =
    ov.fold(List.empty[BasicNameValuePair])(el => List(new BasicNameValuePair(k, mkString(el))))

}

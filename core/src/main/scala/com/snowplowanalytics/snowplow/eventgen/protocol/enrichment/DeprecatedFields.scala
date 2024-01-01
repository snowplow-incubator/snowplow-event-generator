/*
 * Copyright (c) 2021-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.eventgen.protocol.enrichment

import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._

case class DeprecatedFields(
  br_name: Option[String],
  br_family: Option[String],
  br_version: Option[String],
  br_type: Option[String],
  br_renderengine: Option[String],
  os_name: Option[String],
  os_family: Option[String],
  os_manufacturer: Option[String],
  dvce_type: Option[String],
  dvce_ismobile: Option[Boolean],
  etl_tags: Option[String]
)

object DeprecatedFields {
  def gen: Gen[DeprecatedFields] =
    (
      genStringOpt("br_name", 10),
      genStringOpt("br_family", 10),
      genStringOpt("br_version", 10),
      genStringOpt("br_type", 10),
      genStringOpt("br_renderengine", 10),
      genStringOpt("os_name", 10),
      genStringOpt("os_family", 10),
      genStringOpt("os_manufacturer", 10),
      genStringOpt("dvce_type", 10),
      genBoolOpt,
      genStringOpt("etl_tags", 10)
    ).mapN(DeprecatedFields.apply)
}

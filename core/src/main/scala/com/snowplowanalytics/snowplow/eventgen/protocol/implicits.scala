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

import io.circe.{Encoder, Json, JsonObject}
import io.circe.syntax.EncoderOps
import org.scalacheck.Gen

object implicits {

  implicit class GenOps2[A](gen: Gen[A]) {
    def required(implicit enc: Encoder[A]): Gen[Option[Json]] =
      gen.map(v => Some(v.asJson))

    def optional(implicit enc: Encoder[A]): Gen[Option[Json]] =
      Gen.oneOf(
        gen.map(v => Some(v.asJson)),
        Gen.const(None)
      )

    def orNull(implicit enc: Encoder[A]): Gen[Option[Json]] =
      Gen.oneOf(
        gen.map(v => Some(v.asJson)),
        Gen.const(Some(Json.Null))
      )

    def optionalOrNull(implicit enc: Encoder[A]): Gen[Option[Json]] =
      Gen.oneOf(
        gen.map(v => Some(v.asJson)),
        Gen.const(None),
        Gen.const(Some(Json.Null))
      )
  }

  implicit class MapOps(fields: Map[String, Gen[Option[Json]]]) {
    def genObject: Gen[Json] =
      Gen
        .sequence[List[Option[(String, Json)]], Option[(String, Json)]] {
          fields.toList.map { case (name, gen) =>
            gen.map {
              case Some(v) => Some((name, v))
              case None    => None
            }
          }
        }
        .map { fields =>
          JsonObject.fromIterable(fields.collect { case Some(field) => field }).asJson
        }
  }

}

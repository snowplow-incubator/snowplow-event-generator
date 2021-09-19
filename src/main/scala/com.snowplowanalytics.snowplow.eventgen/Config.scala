/*
 * Copyright (c) 2021 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.eventgen

import scala.io.Source

import cats.syntax.either._
import cats.syntax.show._
import cats.syntax.functor._

import cats.effect.Sync

import com.typesafe.config.ConfigFactory

import io.circe.{ Decoder, Json }
import io.circe.generic.semiauto.deriveDecoder

import pureconfig._
import pureconfig.module.circe._
import pureconfig.error.{CannotParse, ConfigReaderFailures}

final case class Config(total: Int,
                        minPerFile: Int,
                        maxPerFile: Int,
                        duplicates: Config.Duplicates)

object Config {
  /** 
   * Duplicate distribution settings
   * @param natPerc percentage of natural duplicates in the whole dataset (0 to 100)
   * @param synPerc percentage of synthetic duplicates in the whole dataset (0 to 100)
   * @param totalDupes exact cardinality of pre-generated duplicate set, e.g. for a dataset
   *                   of 100 events natPerc=10,totalDupes=10 will lean towards 10 duplicates
   *                   each of which is encountered only twice in the dataset, whereas
   *                   natPerc=10,totalDupes=1 will result into 1 event encountered 11 times
   */
  case class Duplicates(natPerc: Int, synPerc: Int, totalDupes: Int)

  implicit val duplicatesDecoder: Decoder[Duplicates] =
    deriveDecoder[Duplicates]
  
  implicit val configDecoder: Decoder[Config] =
    deriveDecoder[Config]

  def fromString(s: String): Either[String, Config] = 
    Either
      .catchNonFatal(ConfigSource.fromConfig(ConfigFactory.parseString(s)))
      .leftMap(error => ConfigReaderFailures(CannotParse(s"Not valid HOCON. ${error.getMessage}", None)))
      .flatMap { config =>
        config
          .load[Json]
          .flatMap { json =>
            json.as[Config].leftMap(failure => ConfigReaderFailures(CannotParse(failure.show, None)))
          }
      }
      .leftMap(_.prettyPrint())

  def fromPath[F[_]: Sync](path: String): F[Either[String, Config]] =
    Sync[F].delay(Source.fromFile(path, "UTF-8").mkString).map(fromString)

}

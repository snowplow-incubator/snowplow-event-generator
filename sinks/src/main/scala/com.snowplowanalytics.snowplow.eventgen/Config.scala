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

import java.nio.file.Path
import java.net.URI
import scala.io.Source
import cats.implicits._
import cats.effect.Sync
import com.monovore.decline._
import com.typesafe.config.ConfigFactory
import io.circe.{Decoder, Json}
import io.circe.generic.semiauto.deriveDecoder
import pureconfig._
import pureconfig.module.circe._
import pureconfig.error.{CannotParse, ConfigReaderFailures}

import scala.util.{Failure, Success}

final case class Config(payloadsTotal: Int,
                        seed: Long,
                        compress: Boolean,
                        eventPerPayloadMax: Int,
                        eventPerPayloadMin: Int,
                        withRaw: Boolean,
                        payloadsPerFile: Int,
                        enrichFormat: Config.EnrichFormat,
                        duplicates: Option[Config.Duplicates],
                       )

object Config {

  /*
    Probability of duplication for natural and synthetic duplicates from 0 to 1
   */
  case class Duplicates(natProb: Float, synProb: Float, natTotal: Int, synTotal: Int)

  sealed trait EnrichFormat

  object EnrichFormat {

    case object Json extends EnrichFormat

    case object Tsv extends EnrichFormat

  }

  implicit val decodeEnrichFormat: Decoder[EnrichFormat] = Decoder.decodeString.emapTry {
    case "json" => Success(EnrichFormat.Json)
    case "tsv" => Success(EnrichFormat.Tsv)
    case _ => Failure(new Exception("format could only be json or tsv"))
  }

  case class Cli(config: Path, output: URI)

  val configOpt = Opts.option[Path]("config", "Path to the configuration HOCON")
  val outputOpt = Opts.option[URI]("output", "Output path")
  val cliOpt = (configOpt, outputOpt).mapN(Cli.apply)
  val application = Command("Snowplow Event Generator", "Generating random manifests of Snowplow events")(cliOpt)

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

  def fromPath[F[_] : Sync](path: String): F[Either[String, Config]] =
    Sync[F].delay(Source.fromFile(path, "UTF-8"))
      .map(_.mkString)
      .map(fromString)

}

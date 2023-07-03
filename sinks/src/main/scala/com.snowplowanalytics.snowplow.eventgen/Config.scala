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
package com.snowplowanalytics.snowplow.eventgen

import java.nio.file.Path
import java.net.URI
import java.time.Instant
import scala.io.Source
import cats.implicits._
import com.monovore.decline._
import com.typesafe.config.{Config => RawConfig, ConfigFactory}
import io.circe.Decoder
import io.circe.config.parser
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

import com.snowplowanalytics.snowplow.eventgen.protocol.event.EventFrequencies
import com.snowplowanalytics.snowplow.eventgen.protocol.event.UnstructEventFrequencies

final case class Config(
  payloadsTotal: Int,
  seed: Long,
  randomisedSeed: Boolean,
  compress: Boolean,
  eventPerPayloadMax: Int,
  eventPerPayloadMin: Int,
  withRaw: Boolean,
  withEnrichedTsv: Boolean,
  withEnrichedJson: Boolean,
  payloadsPerFile: Int,
  duplicates: Option[Config.Duplicates],
  timestamps: Config.Timestamps,
  eventFrequencies: EventFrequencies,
  output: Config.Output
)

object Config {

  /*
    Probability of duplication for natural and synthetic duplicates from 0 to 1
   */
  case class Duplicates(natProb: Float, synProb: Float, natTotal: Int, synTotal: Int)

  implicit val customCodecConfig: Configuration =
    Configuration.default.withDiscriminator("type")

  sealed trait Timestamps
  object Timestamps {
    case object Now extends Timestamps
    case class Fixed(at: Instant) extends Timestamps
  }

  case class Cli(config: Config)

  case class Output(
    file: Option[File] = None,
    kinesis: Option[Kinesis] = None,
    kafka: Option[Kafka] = None,
    pubsub: Option[PubSub] = None
  )

  sealed trait Target
  case class Kinesis(uri: URI, region: Option[String]) extends Target
  case class File(uri: URI) extends Target
  case class PubSub(uri: URI) extends Target
  case class Kafka(brokers: String, topic: String, producerConf: Map[String, String] = Map.empty) extends Target

  val configOpt   = Opts.option[Path]("config", "Path to the configuration HOCON").orNone
  val application = Command("Snowplow Event Generator", "Generating random manifests of Snowplow events")(configOpt)

  // This is needed when providing parameters via system properties
  // e.g. -Dsnowplow.compress=false
  implicit val booleanDecoder: Decoder[Boolean] =
    Decoder.decodeBoolean.or(Decoder.decodeString.emap(_.toBooleanOption.toRight("Invalid boolean")))

  implicit val timestampsConfigDecoder: Decoder[Timestamps] =
    deriveConfiguredDecoder[Timestamps]

  implicit val duplicatesDecoder: Decoder[Duplicates] =
    deriveConfiguredDecoder[Duplicates]

  implicit val frequenciesDecoder: Decoder[EventFrequencies] =
    deriveConfiguredDecoder[EventFrequencies]

  implicit val unstructEventFrequenciesDecoder: Decoder[UnstructEventFrequencies] =
    deriveConfiguredDecoder[UnstructEventFrequencies]

  implicit val mapDecoder: Decoder[Map[String, String]] = Decoder.decodeMap[String, String]

  implicit val uriDecoder: Decoder[URI] = Decoder[String].emap { str =>
    Either.catchOnly[IllegalArgumentException](URI.create(str)).leftMap(_.getMessage)
  }

  implicit val kafkaDecoder: Decoder[Kafka] =
    deriveConfiguredDecoder[Kafka]

  implicit val kinesisDecoder: Decoder[Kinesis] =
    deriveConfiguredDecoder[Kinesis]

  implicit val fileDecoder: Decoder[File] =
    deriveConfiguredDecoder[File]

  implicit val pubSubDecoder: Decoder[PubSub] =
    deriveConfiguredDecoder[PubSub]

  implicit val outputDecoder: Decoder[Output] =
    deriveConfiguredDecoder[Output]

  implicit val configDecoder: Decoder[Config] =
    deriveConfiguredDecoder[Config]

  /** Parse raw CLI arguments into validated and transformed application config
    *
    * @param argv
    *   list of command-line arguments, including optionally a `--config` argument
    * @return
    *   The parsed config using the provided file and the standard typesafe config loading process. See
    *   https://github.com/lightbend/config/tree/v1.4.1#standard-behavior Or an error message if config could not be
    *   loaded.
    */
  def parse(argv: Seq[String]): Either[String, Cli] =
    application.parse(argv).leftMap(_.show).flatMap {
      case Some(path) =>
        for {
          raw    <- loadFromFile(path)
          parsed <- parser.decode[Config](raw).leftMap(e => s"Could not parse config $path: ${e.show}")
        } yield Cli(parsed)
      case _ => Left(s"Could not resolve config without a provided hocon file")
    }

  /** Uses the typesafe config layering approach. Loads configurations in the following priority order:
    *   1. System properties 2. The provided configuration file 3. application.conf of our app 4. reference.conf of any
    *      libraries we use
    */
  def loadFromFile(file: Path): Either[String, RawConfig] =
    for {
      text <- Either
        .catchNonFatal(Source.fromFile(file.toFile).mkString)
        .leftMap(e => s"Could not read config file: ${e.getMessage}")
      resolved <- Either
        .catchNonFatal(ConfigFactory.parseString(text).resolve)
        .leftMap(e => s"Could not parse config file $file: ${e.getMessage}")
    } yield namespaced(ConfigFactory.load(namespaced(resolved.withFallback(namespaced(ConfigFactory.load())))))

  /** Optionally give precedence to configs wrapped in a "snowplow" block. To help avoid polluting config namespace */
  private def namespaced(config: RawConfig): RawConfig =
    if (config.hasPath(Namespace))
      config.getConfig(Namespace).withFallback(config.withoutPath(Namespace))
    else
      config

  private val Namespace = "snowplow"

}

/*
 * Copyright (c) 2021-2025 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.snowplow.eventgen.GenConfig

final case class Config(
  events: GenConfig.Events,
  output: Config.Output,
  eventsTotal: Option[Long],
  timestamp: Config.Timestamp,
  appId: Option[String],
  seed: Option[Long],
  eventsPerPayload: GenConfig.EventsPerPayload,
  eventsFrequencies: GenConfig.EventsFrequencies,
  contextsPerEvent: GenConfig.ContextsPerEvent,
  duplicates: Option[GenConfig.Duplicates],
  rate: Option[GenConfig.Rate],
  userGraph: Option[GenConfig.UserGraph],
  appProfiles: Option[List[GenConfig.AppProfile]]
) {
  // Helper to get active user configuration
  def activeUserConfig: Option[Either[GenConfig.UserGraph, List[GenConfig.AppProfile]]] =
    (userGraph, appProfiles) match {
      case (Some(single), None)   => Some(Left(single))
      case (None, Some(profiles)) => Some(Right(profiles))
      case (Some(_), Some(_)) =>
        throw new IllegalArgumentException("Cannot specify both userGraph and appProfiles")
      case (None, None) => None
    }
}

object Config {

  case class Cli(config: Config)

  val configOpt   = Opts.option[Path]("config", "Path to the configuration HOCON").orNone
  val application = Command("Snowplow Event Generator", "Generating and sending random Snowplow events")(configOpt)

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
      case _ => Left("Could not resolve config without a provided hocon file")
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

  private val Namespace = "snowplow"

  /** Optionally give precedence to configs wrapped in a "snowplow" block. To help avoid polluting config namespace */
  private def namespaced(config: RawConfig): RawConfig =
    if (config.hasPath(Namespace))
      config.getConfig(Namespace).withFallback(config.withoutPath(Namespace))
    else
      config

  implicit val customCodecConfig: Configuration =
    Configuration.default.withDiscriminator("type").withDefaults

  sealed trait Output
  object Output {
    case class Kinesis(streamName: String, region: Option[String]) extends Output
    case class PubSub(topic: String) extends Output
    case class Kafka(brokers: String, topic: String, producerConf: Map[String, String] = Map.empty) extends Output
    case class Http(endpoint: org.http4s.Uri) extends Output
    case class File(path: URI, eventsPerFile: Int, compress: Boolean) extends Output
    case object Stdout extends Output
  }

  sealed trait Timestamp
  object Timestamp {
    case object Now extends Timestamp
    case class Fixed(at: Instant) extends Timestamp
  }

  // This is needed when providing parameters via system properties
  // e.g. -Dsnowplow.file.compress=false
  implicit val booleanDecoder: Decoder[Boolean] =
    Decoder.decodeBoolean.or(Decoder.decodeString.emap(_.toBooleanOption.toRight("Invalid boolean")))

  implicit val httpUriDecoder: Decoder[org.http4s.Uri] = Decoder[String].emap { str =>
    org.http4s.Uri.fromString(str).leftMap(_.getMessage)
  }

  implicit val mapDecoder: Decoder[Map[String, String]] = Decoder.decodeMap[String, String]

  implicit val uriDecoder: Decoder[URI] = Decoder[String].emap { str =>
    Either.catchOnly[IllegalArgumentException](URI.create(str)).leftMap(_.getMessage)
  }

  implicit val enrichedFormatDecoder: Decoder[GenConfig.Events.Enriched.Format] =
    deriveConfiguredDecoder[GenConfig.Events.Enriched.Format]

  implicit val methodFrequenciesDecoder: Decoder[GenConfig.Events.Http.MethodFrequencies] =
    deriveConfiguredDecoder[GenConfig.Events.Http.MethodFrequencies]

  implicit val eventsDecoder: Decoder[GenConfig.Events] =
    deriveConfiguredDecoder[GenConfig.Events]

  implicit val kinesisDecoder: Decoder[Output.Kinesis] =
    deriveConfiguredDecoder[Output.Kinesis]

  implicit val pubSubDecoder: Decoder[Output.PubSub] =
    deriveConfiguredDecoder[Output.PubSub]

  implicit val kafkaDecoder: Decoder[Output.Kafka] =
    deriveConfiguredDecoder[Output.Kafka]

  implicit val httpDecoder: Decoder[Output.Http] =
    deriveConfiguredDecoder[Output.Http]

  implicit val fileDecoder: Decoder[Output.File] =
    deriveConfiguredDecoder[Output.File]

  implicit val outputDecoder: Decoder[Output] =
    deriveConfiguredDecoder[Output]

  implicit val timestampDecoder: Decoder[Timestamp] =
    deriveConfiguredDecoder[Timestamp]

  implicit val eventsPerPayloadDecoder: Decoder[GenConfig.EventsPerPayload] =
    deriveConfiguredDecoder[GenConfig.EventsPerPayload]

  implicit val eventsFrequenciesDecoder: Decoder[GenConfig.EventsFrequencies] =
    deriveConfiguredDecoder[GenConfig.EventsFrequencies]

  implicit val contextsPerEventDecoder: Decoder[GenConfig.ContextsPerEvent] =
    deriveConfiguredDecoder[GenConfig.ContextsPerEvent]
      .ensure(_.min >= 0, "minPerEvent must be a positive number")
      .ensure(_.max >= 0, "minPerEvent must be a positive number")
      .ensure(c => c.max >= c.min, "minPerEvent cannot be larger than maxPerEvent")

  implicit val duplicatesDecoder: Decoder[GenConfig.Duplicates] =
    deriveConfiguredDecoder[GenConfig.Duplicates]

  implicit val rateDecoder: Decoder[GenConfig.Rate] =
    deriveConfiguredDecoder[GenConfig.Rate]

  implicit val userGraphDistributionDecoder: Decoder[GenConfig.UserGraph.Distribution] =
    deriveConfiguredDecoder[GenConfig.UserGraph.Distribution]

  implicit val userGraphDecoder: Decoder[GenConfig.UserGraph] =
    deriveConfiguredDecoder[GenConfig.UserGraph]

  implicit val appProfileDecoder: Decoder[GenConfig.AppProfile] =
    deriveConfiguredDecoder[GenConfig.AppProfile]

  implicit val configDecoder: Decoder[Config] =
    deriveConfiguredDecoder[Config]
}

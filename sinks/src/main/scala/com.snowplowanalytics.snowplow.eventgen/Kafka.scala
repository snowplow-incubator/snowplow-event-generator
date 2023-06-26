/*
 * Copyright (c) 2021-2023 Snowplow Analytics Ltd. All rights reserved.
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

import fs2.{Pipe, Stream}
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerSettings, Serializer}
import cats.syntax.all._
import cats.effect.Async
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.util.parsing.combinator.RegexParsers

object Kafka {
  case class Config(topic: String, brokers: String, producerConf: Map[String, String])
  object Properties extends RegexParsers {
    def topic: Parser[String]   = "[^?]+".r
    def brokers: Parser[String] = "brokers=" ~> value
    def key: Parser[String]     = "[^=]+".r
    def value: Parser[String]   = opt("\"") ~> """([^"\\&]|\\.)*""".r <~ opt("\"")
    def custom: Parser[Map[String, String]] = key ~ "=" ~ value ^^ { case k ~ _ ~ v =>
      Map(k -> v)
    }
    def config: Parser[Config] = "kafka://" ~ topic ~ "?" ~ brokers ~ opt("&") ~ repsep(custom, "&") ^^ {
      case _ ~ t ~ _ ~ b ~ _ ~ c =>
        Config(t, b, c.flatten.toMap)
    }
    def apply(s: String) = {
      println(s)
      parseAll(config, s) match {
        case Success(p, _) => Right(p)
        case Failure(f, b) => Left(s"Incomplete or invalid Kafka URI at ${b.pos}: $f")

        case Error(e, _) => Left(s"Unable to parse Kafka URI: $e")

      }
    }
  }

  def toProducerRecord(topicName: String, record: Event): ProducerRecord[String, Event] =
    ProducerRecord(topicName, UUID.randomUUID().toString, record)

  def sink[F[_]: Async](properties: Config): Pipe[F, Main.GenOutput, Unit] = {
    implicit val serializer = Serializer.lift[F, Event](e => Async[F].pure(e.toTsv.getBytes(StandardCharsets.UTF_8)))

    def writeKafka(properties: Config): Pipe[F, Event, Unit] = {
      val batchSize = 100
      _.map(toProducerRecord(properties.topic, _))
        .chunkN(batchSize, allowFewer = true)
        .through(
          KafkaProducer.pipe(
            ProducerSettings[F, String, Event]
              .withBootstrapServers(properties.brokers)
              .withProperties(
                ("key.serializer", "org.apache.kafka.common.serialization.StringSerializer"),
                ("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
              )
              .withProperties(properties.producerConf)
          )
        )
        .chunkLimit(batchSize)
        .void
    }

    st: Stream[F, Main.GenOutput] => st.map(_._2).flatMap(Stream.emits).through(writeKafka(properties))
  }

}

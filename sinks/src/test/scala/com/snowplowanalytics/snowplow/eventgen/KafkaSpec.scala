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

import java.nio.charset.StandardCharsets
import org.specs2.mutable.Specification

class KafkaSpec extends Specification {

  "Kafka.Properties" should {
    "load brokers and values from an URI" in {
      val topic   = "my-topic"
      val brokers = "lorem:1234,ipsum:5678"
      val key     = "my-key"
      val value   = "my-value"

      val props = Kafka.Properties(s"kafka://${topic}?brokers=${brokers}&${key}=${value}")
      props must beRight()

      props.map(_.brokers) must beRight(brokers)
      props.map(_.producerConf).toOption.get must havePairs(key -> value)
    }
    "load full properties from an EventHubs connection string" in {
      val jaas = java
        .net
        .URLEncoder
        .encode(
          "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=Endpoint=sb://peel-dev.servicebus.windows.net/;SharedAccessKeyName=standard;SharedAccessKey=randomkey;EntityPath=events",
          StandardCharsets.UTF_8.toString()
        )
      val url = java
        .net
        .URI
        .create(
          s"""kafka://peel-dev?brokers=lorem.servicebus.windows.net:443&security.protocol=SASL_SSL&sasl.mechanism=PLAIN&sasl.jaas.config=$jaas"""
        )
      val props = Kafka.Properties(url.toString)
      props must beRight()

      props.map(_.brokers) must beRight("lorem.servicebus.windows.net:443")
      props.map(_.producerConf).toOption.get must havePairs(
        "security.protocol" -> "SASL_SSL",
        "sasl.mechanism"    -> "PLAIN",
        "sasl.jaas.config" -> """org.apache.kafka.common.security.plain.PlainLoginModule+required+username%3D%22%24ConnectionString%22+password%3DEndpoint%3Dsb%3A%2F%2Fpeel-dev.servicebus.windows.net%2F%3BSharedAccessKeyName%3Dstandard%3BSharedAccessKey%3Drandomkey%3BEntityPath%3Devents"""
      )
    }
  }
}

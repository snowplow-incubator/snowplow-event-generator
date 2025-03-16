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

import org.specs2.mutable.Specification

class ConfigSpec extends Specification {
  "Config.loadFromFile" should {
    "succeed with the example config" in {
      Config.parse(Seq("--config", "config/config.example.hocon")) must beRight()
    }

    "fail with application.conf (it doesn't have all required fields)" in {
      Config.parse(Seq("--config", "sinks/src/main/resources/application.conf")) must beLeft.like { case failure =>
        failure must contain("DecodingFailure at .events: Missing required field")
      }
    }
  }
}

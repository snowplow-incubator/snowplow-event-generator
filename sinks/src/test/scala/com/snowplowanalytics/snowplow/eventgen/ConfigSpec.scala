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

import java.nio.file.Paths

class ConfigSpec extends Specification {
  "Config.loadFromFile" should {
    "succeed with the default application.conf" in {
      val valid = Paths.get("sinks/src/main/resources/application.conf")
      val result = Config.loadFromFile(valid)

      result must beRight()
    }

    "fail with invalid HOCON" in {
      val invalid = Paths.get("sinks/src/test/resources/invalid.conf")
      val result = Config.loadFromFile(invalid)

      result must beLeft()
    }
  }
}

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

import com.snowplowanalytics.snowplow.eventgen.protocol.Body
import org.specs2.mutable.Specification
import java.time.Instant

/** Integration tests for enriched event generation.
  *
  * Tests the full pipeline from UserGraph config → Body → enriched events to ensure all features work end-to-end.
  */
class EnrichedEventIntegrationSpec extends Specification {

  "Enriched event generation with UserGraph" should {

    "populate user_id at configured authenticationRate" in {
      val config = GenConfig.UserGraph(
        numUsers = 1000L,
        sharedIdentifierRate = 0.05,
        identifiersPerUser = Map("cookie" -> 3, "device" -> 2, "ip" -> 2),
        authenticationRate = 0.80,
        distribution = GenConfig.UserGraph.Distribution.Uniform,
        activeUserRate = 1.0,
        usersPerCluster = 5
      )

      val samples = (1 to 500).map { _ =>
        Body
          .gen(
            Instant.now(),
            GenConfig.EventsFrequencies(1, 0, 0, 0, 0, 0, 0, Map.empty),
            GenConfig.ContextsPerEvent(0, 0),
            Some(config)
          )
          .sample
          .get
      }

      val withUserId = samples.flatMap(_.u).count(_.uid.isDefined)
      val actualRate = withUserId.toDouble / samples.size

      // Should be close to 80% (within 10% tolerance for 500 samples)
      actualRate must beCloseTo(0.80, 0.10)
    }

    "populate user_id = None when authenticationRate = 0.0" in {
      val config = GenConfig.UserGraph(
        numUsers = 1000L,
        sharedIdentifierRate = 0.05,
        identifiersPerUser = Map("cookie" -> 3),
        authenticationRate = 0.0,
        distribution = GenConfig.UserGraph.Distribution.Uniform,
        activeUserRate = 1.0,
        usersPerCluster = 5
      )

      val samples = (1 to 100).map { _ =>
        Body
          .gen(
            Instant.now(),
            GenConfig.EventsFrequencies(1, 0, 0, 0, 0, 0, 0, Map.empty),
            GenConfig.ContextsPerEvent(0, 0),
            Some(config)
          )
          .sample
          .get
      }

      val withUserId = samples.flatMap(_.u).count(_.uid.isDefined)

      withUserId must_== 0
    }

    "populate user_id for all events when authenticationRate = 1.0" in {
      val config = GenConfig.UserGraph(
        numUsers = 1000L,
        sharedIdentifierRate = 0.05,
        identifiersPerUser = Map("cookie" -> 3),
        authenticationRate = 1.0,
        distribution = GenConfig.UserGraph.Distribution.Uniform,
        activeUserRate = 1.0,
        usersPerCluster = 5
      )

      val samples = (1 to 100).map { _ =>
        Body
          .gen(
            Instant.now(),
            GenConfig.EventsFrequencies(1, 0, 0, 0, 0, 0, 0, Map.empty),
            GenConfig.ContextsPerEvent(0, 0),
            Some(config)
          )
          .sample
          .get
      }

      val withUserId = samples.flatMap(_.u).count(_.uid.isDefined)

      withUserId must_== 100
    }

    "generate consistent user_id format" in {
      val config = GenConfig.UserGraph(
        numUsers = 1000L,
        sharedIdentifierRate = 0.05,
        identifiersPerUser = Map("cookie" -> 3),
        authenticationRate = 1.0,
        distribution = GenConfig.UserGraph.Distribution.Uniform,
        activeUserRate = 1.0,
        usersPerCluster = 5
      )

      val sample = Body
        .gen(
          Instant.now(),
          GenConfig.EventsFrequencies(1, 0, 0, 0, 0, 0, 0, Map.empty),
          GenConfig.ContextsPerEvent(0, 0),
          Some(config)
        )
        .sample
        .get

      sample.u.flatMap(_.uid) must beSome[String].which(_.startsWith("user_"))
    }
  }
}

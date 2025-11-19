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

import org.scalacheck.Gen

/** Deterministic user selection with power law distribution.
  */
object UserDistribution {

  /** Select user using power law distribution (80/20 Pareto principle). Top 20% of users generate ~80% of events.
    *
    * O(1) time complexity, works efficiently with large identity pools.
    *
    * @param numUsers
    *   Total number of identities in the pool
    * @param hotFraction
    *   Fraction of identities that are "hot" (default 0.2 = 20%)
    * @param hotEventFraction
    *   Fraction of events from hot identities (default 0.8 = 80%)
    * @return
    *   Gen that produces identity IDs following power law distribution
    */
  def selectPowerLaw(
    numUsers: Long,
    hotFraction: Double = 0.2,
    hotEventFraction: Double = 0.8
  ): Gen[Long] =
    Gen.choose(0.0, 1.0).map { u =>
      if (u < hotEventFraction) {
        // 80% of events -> top 20% of identities
        // Use power 0.15 for very strong concentration (15th root)
        val hotPoolSize = (numUsers.toDouble * hotFraction).toLong
        val position    = Math.pow(u / hotEventFraction, 0.15)
        (position * hotPoolSize.toDouble).toLong
      } else {
        // 20% of events -> bottom 80% of identities
        // Uniform distribution in cold zone
        val hotPoolSize  = (numUsers.toDouble * hotFraction).toLong
        val coldPoolSize = numUsers - hotPoolSize
        val position     = (u - hotEventFraction) / (1.0 - hotEventFraction)
        hotPoolSize + (position * coldPoolSize.toDouble).toLong
      }
    }
}

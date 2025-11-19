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

/** Utility for selecting app profiles and determining identity-app affinity.
  *
  * Supports multi-profile scenarios where different apps (website, mobile app, etc.) share the same underlying identity
  * pool but have different characteristics.
  */
object AppProfileSelector {

  /** Hash constants for deterministic app-user affinity.
    *
    *   - KnuthHash: 2^32 / golden ratio for multiplicative hashing
    *   - NormalizationBase: Denominator for converting hash to [0,1] probability
    *   - SeedMultiplier: Prime for combining userId and appId (shared with ClusterAlgorithm)
    */
  private val KnuthHash         = 2654435761L
  private val NormalizationBase = 1000000
  private val SeedMultiplier    = 31L // Same as ClusterAlgorithm.HashConstants.ClusterIdPrime

  /** Select a profile using weighted random selection.
    *
    * @param profiles
    *   List of app profiles with weights
    * @return
    *   Gen that selects a profile based on weights
    */
  def selectProfile(profiles: List[GenConfig.AppProfile]): Gen[GenConfig.AppProfile] = {
    val weightedChoices = profiles.map(p => (p.weight, Gen.const(p)))
    Gen.frequency(weightedChoices: _*)
  }

  /** Determine if a specific identity uses a given app.
    *
    * Uses deterministic hash of identity ID and app ID to decide. This ensures the same identity always makes the same
    * decision for a given app.
    *
    * @param userId
    *   The identity cluster ID
    * @param appId
    *   The app identifier
    * @param usageRate
    *   Probability this identity uses this app (0.0 to 1.0)
    * @return
    *   true if this identity uses this app
    */
  def doesIdentityUseApp(userId: Long, appId: String, usageRate: Double): Boolean = {
    val seed       = userId * SeedMultiplier              + appId.hashCode.toLong
    val hash       = ((seed * KnuthHash) & Long.MaxValue) % NormalizationBase
    val normalized = hash.toDouble                        / NormalizationBase.toDouble
    normalized < usageRate
  }

  /** Select an identity that uses the given app.
    *
    * Uses a Gen-based filter approach that works efficiently with both uniform and power-law distributions. Generates a
    * batch of candidates and selects the first match to avoid retryUntil limitations.
    *
    * @param numUsers
    *   Total number of identities in the pool
    * @param appId
    *   The app identifier
    * @param usageRate
    *   Probability an identity uses this app
    * @param userSelector
    *   Gen that selects user IDs (e.g., uniform or power law)
    * @return
    *   Gen that selects an identity ID that uses this app
    */
  def selectIdentityForApp(
    numUsers: Long,
    appId: String,
    usageRate: Double,
    userSelector: Gen[Long]
  ): Gen[Long] = {
    require(usageRate > 0.0, s"usageRate must be > 0.0 for app '$appId' (got $usageRate)")

    val expectedMatches = numUsers * usageRate
    require(
      expectedMatches >= 10.0,
      s"Too few expected matches for app '$appId': $expectedMatches " +
        s"(numUsers=$numUsers × usageRate=$usageRate). " +
        s"Need ≥10 expected matches to avoid acceptance sampling failure. " +
        s"Either increase numUsers or increase usageRate."
    )

    // Generate batch of candidates and find first match
    // Batch size: 5 / usageRate ensures >99.9% probability of at least one match
    // E.g., usageRate=0.2, batchSize=25: P(all miss) = 0.8^25 ≈ 0.004%
    val batchSize = Math.max(50, (5.0 / usageRate).toInt)

    Gen.listOfN(batchSize, userSelector).map { candidates =>
      candidates.find(id => doesIdentityUseApp(id, appId, usageRate)).getOrElse {
        throw new IllegalStateException(
          s"Failed to find user for app '$appId' after $batchSize attempts " +
            s"(usageRate=$usageRate, expectedMatches=${expectedMatches}). " +
            s"This should never happen with proper batch sizing."
        )
      }
    }
  }
}

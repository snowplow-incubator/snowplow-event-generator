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

import com.snowplowanalytics.snowplow.eventgen.protocol.common.ClusterAlgorithm.HashConstants
import org.scalacheck.Gen

/** Utility for selecting app profiles and determining identity-app affinity.
  *
  * Supports multi-profile scenarios where different apps (website, mobile app, etc.) share the same underlying identity
  * pool but have different characteristics.
  */
object AppProfileSelector {

  /** Minimum number of candidates to generate per batch when filtering users by app. Set to 50 to ensure reasonable
    * success probability even with low usage rates, while avoiding excessive memory allocation for small batches.
    */
  private val MinBatchSize = 50

  /** Multiplier for batch size calculation: batchSize = max(MinBatchSize, BatchSizeMultiplier / usageRate). Value of
    * 5.0 means we generate ~5x more candidates than statistically needed, providing >99% success probability per batch
    * for typical usage rates.
    */
  private val BatchSizeMultiplier = 5.0

  /** Minimum expected number of matching users required for acceptance sampling to work reliably. With fewer than 10
    * expected matches, the probability of finding no match in a batch becomes unacceptably high, leading to potential
    * runtime failures.
    */
  private val MinExpectedMatches = 10.0

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
    val seed = userId * HashConstants.ClusterIdPrime                         + appId.hashCode.toLong
    val hash = ((seed * HashConstants.KnuthHashGoldenRatio) & Long.MaxValue) % HashConstants.NormalizationBase
    val normalized = hash.toDouble / HashConstants.NormalizationBase.toDouble
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
      expectedMatches >= MinExpectedMatches,
      s"Too few expected matches for app '$appId': $expectedMatches " +
        s"(numUsers=$numUsers × usageRate=$usageRate). " +
        s"Need ≥$MinExpectedMatches expected matches to avoid acceptance sampling failure. " +
        s"Either increase numUsers or increase usageRate."
    )

    val batchSize = Math.max(MinBatchSize, (BatchSizeMultiplier / usageRate).toInt)

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

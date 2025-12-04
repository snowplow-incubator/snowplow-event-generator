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

import com.snowplowanalytics.snowplow.eventgen.protocol.common.User
import org.scalacheck.Gen

/** Helper for generating correlated user data.
  *
  * Generates User and authenticated user_id from the same user, ensuring proper correlation without mutable state.
  */
object CorrelatedUser {

  /** Correlated identity data for a single event.
    *
    * @param userId
    *   The identity cluster ID
    * @param user
    *   The User object with cookies, devices, IPs, etc.
    * @param authenticatedUserId
    *   Optional authenticated user_id (respects authenticationRate)
    */
  case class Data(
    userId: Long,
    user: User,
    authenticatedUserId: Option[String]
  )

  /** Generate correlated identity data using algorithmic approach. Fast even with large identity pools - computes
    * cluster membership on-demand.
    *
    * @param config
    *   Identity graph configuration
    * @param appId
    *   Optional app ID for multi-profile scenarios
    * @return
    *   Gen that produces correlated identity data
    */
  def gen(config: GenConfig.UserGraph, appId: Option[String]): Gen[Data] =
    for {
      userId     <- selectIdentityForApp(config, appId)
      user       <- User.genFromIdentity(userId, config)
      authUserId <- genAuthenticatedUserId(userId, config)
    } yield Data(userId, user, authUserId)

  /** Select an identity ID that uses the given app (if specified).
    *
    * For multi-profile scenarios, uses acceptance sampling to find an identity that uses the specified app. For
    * single-profile scenarios, selects any identity.
    */
  private def selectIdentityForApp(
    config: GenConfig.UserGraph,
    appId: Option[String]
  ): Gen[Long] = {
    // Create user selector based on distribution config
    val userSelector = config.distribution match {
      case GenConfig.UserGraph.Distribution.PowerLaw =>
        UserDistribution.selectPowerLaw(config.numUsers)
      case GenConfig.UserGraph.Distribution.Uniform =>
        Gen.choose(0L, config.numUsers - 1)
    }

    appId match {
      case Some(app) =>
        // Filter user selector to only users who use this app
        AppProfileSelector.selectIdentityForApp(
          config.numUsers,
          app,
          config.activeUserRate,
          userSelector
        )
      case None =>
        // Use user selector directly
        userSelector
    }
  }

  /** Generate authenticated user_id for an identity, respecting authentication rate.
    *
    * @param userId
    *   The identity cluster ID
    * @param config
    *   Identity graph configuration
    * @return
    *   Gen that produces Some(userId) with probability = authenticationRate, None otherwise
    */
  private def genAuthenticatedUserId(
    userId: Long,
    config: GenConfig.UserGraph
  ): Gen[Option[String]] =
    Gen.choose(0.0, 1.0).map { roll =>
      if (roll < config.authenticationRate) {
        Some(s"user_${userId}")
      } else {
        None
      }
    }
}

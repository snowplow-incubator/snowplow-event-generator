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

object GenConfig {

  sealed trait Events
  object Events {
    case object CollectorPayloads extends Events
    case class Enriched(format: Enriched.Format, generateEnrichments: Boolean) extends Events
    case class Http(
      methodFrequencies: Option[Http.MethodFrequencies]
    ) extends Events

    object Enriched {
      sealed trait Format
      object Format {
        case object TSV extends Format
        case object JSON extends Format
      }
    }

    object Http {
      case class MethodFrequencies(get: Int, post: Int, head: Int)
    }
  }

  case class EventsPerPayload(min: Int, max: Int)

  case class EventsFrequencies(
    struct: Int,
    unstruct: Int,
    pageView: Int,
    pagePing: Int,
    transaction: Int,
    transactionItem: Int,
    unstructEventFrequencyDefault: Int,
    unstructEventFrequencies: Map[String, Int]
  )

  case class ContextsPerEvent(min: Int, max: Int)

  case class Duplicates(natProb: Float, synProb: Float, natTotal: Int, synTotal: Int)

  final case class Rate(eventsPerSecond: Int, tickMillis: Int = 100)

  /** User graph configuration using merge-based model.
    *
    * Users are assigned to clusters where they share identifiers. The observable shared identifier rate will exactly
    * match the configured sharedIdentifierRate.
    */
  case class UserGraph(
    numUsers: Long,
    sharedIdentifierRate: Double, // Fraction of users who share identifiers with others (exact)
    identifiersPerUser: Map[String, Int],
    authenticationRate: Double = 0.80,
    distribution: UserGraph.Distribution = UserGraph.Distribution.PowerLaw,
    activeUserRate: Double = 1.0, // For multi-app: what % of user pool is active on this app
    usersPerCluster: Int = 5,     // Average number of users per cluster
    numSharedIdentifiers: Option[Map[String, Int]] =
      None // Optional: How many identifiers to share per type (default: 1)
  ) {
    def maxIdentifiers(idType: String): Int =
      identifiersPerUser.getOrElse(idType, 1)

    /** Get number of shared identifiers for a given identifier type. Defaults to 1 (guarantees observable rate =
      * configured sharedIdentifierRate)
      */
    def sharedIdentifiersForType(idType: String): Int =
      numSharedIdentifiers.flatMap(_.get(idType)).getOrElse(1)
  }

  /** App profile for multi-app scenarios.
    *
    * Allows different apps (website, mobile app, etc.) to have different identity characteristics while sharing the
    * same underlying identity pool.
    */
  case class AppProfile(
    appId: String,
    weight: Int,
    userGraph: UserGraph
  )

  object UserGraph {

    sealed trait Distribution
    object Distribution {
      case object Uniform extends Distribution
      case object PowerLaw extends Distribution
    }
  }

  sealed trait IdentitySource
  object IdentitySource {
    case object NoIdentity extends IdentitySource
    case class SingleGraph(config: UserGraph) extends IdentitySource
    case class ProfileGraph(appId: String, config: UserGraph) extends IdentitySource
  }
}

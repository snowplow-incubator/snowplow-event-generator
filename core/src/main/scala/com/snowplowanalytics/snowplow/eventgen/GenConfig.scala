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
    case class Http(methodFrequencies: Option[Http.MethodFrequencies]) extends Events

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
}

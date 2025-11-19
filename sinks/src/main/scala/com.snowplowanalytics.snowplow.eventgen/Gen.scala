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

import java.time.Instant

import org.scalacheck.{Gen => ScalaGen}

import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload
import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent
import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest
import com.snowplowanalytics.snowplow.eventgen.GenConfig

object Gen {

  def collectorPayload(config: Config, time: Instant): ScalaGen[CollectorPayload] =
    config.activeUserConfig match {
      case Some(Right(profiles)) =>
        // Multi-profile mode: select a profile for each event (uses algorithmic approach)
        for {
          profile <- AppProfileSelector.selectProfile(profiles)
          payload <- CollectorPayload.genWithProfile(
            config.eventsPerPayload,
            time,
            config.eventsFrequencies,
            config.contextsPerEvent,
            profile.appId,
            profile.userGraph
          )
        } yield payload

      case Some(Left(singleGraph)) =>
        // Single graph mode (uses algorithmic approach)
        config.duplicates match {
          case Some(dups) =>
            CollectorPayload.genDup(
              dups,
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              Some(singleGraph)
            )
          case None =>
            CollectorPayload.gen(
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              Some(singleGraph)
            )
        }

      case None =>
        // No identity graph: random generation
        config.duplicates match {
          case Some(dups) =>
            CollectorPayload.genDup(
              dups,
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              None
            )
          case None =>
            CollectorPayload.gen(
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              None
            )
        }
    }

  def enriched(
    config: Config,
    time: Instant,
    format: GenConfig.Events.Enriched.Format,
    generateEnrichments: Boolean
  ): ScalaGen[List[String]] = {
    val gen = config.activeUserConfig match {
      case Some(Right(profiles)) =>
        // Multi-profile mode: select a profile for each event
        for {
          profile <- AppProfileSelector.selectProfile(profiles)
          events <- SdkEvent.genWithProfile(
            config.eventsPerPayload,
            time,
            config.eventsFrequencies,
            config.contextsPerEvent,
            generateEnrichments,
            profile.appId,
            profile.userGraph
          )
        } yield events

      case Some(Left(singleGraph)) =>
        // Single profile mode (backward compatible)
        config.duplicates match {
          case Some(dups) =>
            SdkEvent.genDup(
              dups,
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              generateEnrichments,
              config.appId,
              Some(singleGraph)
            )
          case None =>
            SdkEvent.gen(
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              generateEnrichments,
              config.appId,
              Some(singleGraph)
            )
        }

      case None =>
        // No identity graph: random generation
        config.duplicates match {
          case Some(dups) =>
            SdkEvent.genDup(
              dups,
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              generateEnrichments,
              config.appId,
              None
            )
          case None =>
            SdkEvent.gen(
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              generateEnrichments,
              config.appId,
              None
            )
        }
    }

    gen.map(_.map { e =>
      format match {
        case GenConfig.Events.Enriched.Format.TSV  => e.toTsv
        case GenConfig.Events.Enriched.Format.JSON => e.toJson(true).noSpaces
      }
    })
  }

  def httpRequest(
    config: Config,
    time: Instant,
    methodFrequencies: Option[GenConfig.Events.Http.MethodFrequencies],
    validEventsOnly: Boolean
  ): ScalaGen[HttpRequest] =
    config.activeUserConfig match {
      case Some(Right(profiles)) =>
        // Multi-profile mode: select a profile for each event
        for {
          profile <- AppProfileSelector.selectProfile(profiles)
          request <- config.duplicates match {
            case Some(dups) =>
              HttpRequest.genDup(
                dups,
                config.eventsPerPayload,
                time,
                config.eventsFrequencies,
                config.contextsPerEvent,
                methodFrequencies,
                Some(profile.userGraph),
                validEventsOnly
              )
            case None =>
              HttpRequest.gen(
                config.eventsPerPayload,
                time,
                config.eventsFrequencies,
                config.contextsPerEvent,
                methodFrequencies,
                Some(profile.userGraph),
                validEventsOnly
              )
          }
        } yield request

      case Some(Left(singleGraph)) =>
        // Single graph mode
        config.duplicates match {
          case Some(dups) =>
            HttpRequest.genDup(
              dups,
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              methodFrequencies,
              Some(singleGraph),
              validEventsOnly
            )
          case None =>
            HttpRequest.gen(
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              methodFrequencies,
              Some(singleGraph),
              validEventsOnly
            )
        }

      case None =>
        // No identity graph: random generation
        config.duplicates match {
          case Some(dups) =>
            HttpRequest.genDup(
              dups,
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              methodFrequencies,
              None,
              validEventsOnly
            )
          case None =>
            HttpRequest.gen(
              config.eventsPerPayload,
              time,
              config.eventsFrequencies,
              config.contextsPerEvent,
              methodFrequencies,
              None,
              validEventsOnly
            )
        }
    }
}

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

object Gen {

  private def resolveIdentitySource(
    profiles: List[GenConfig.AppProfile]
  ): ScalaGen[GenConfig.IdentitySource] =
    AppProfileSelector.selectProfile(profiles).map { profile =>
      GenConfig.IdentitySource.ProfileGraph(profile.appId, profile.userGraph)
    }

  private def getIdentitySource(config: Config): ScalaGen[GenConfig.IdentitySource] =
    config.activeUserConfig match {
      case Some(Right(profiles))   => resolveIdentitySource(profiles)
      case Some(Left(singleGraph)) => ScalaGen.const(GenConfig.IdentitySource.SingleGraph(singleGraph))
      case None                    => ScalaGen.const(GenConfig.IdentitySource.NoIdentity)
    }

  def collectorPayload(config: Config, time: Instant): ScalaGen[CollectorPayload] =
    for {
      identitySource <- getIdentitySource(config)
      payload <- CollectorPayload.gen(
        config.eventsPerPayload,
        time,
        config.eventsFrequencies,
        config.contextsPerEvent,
        identitySource,
        config.duplicates
      )
    } yield payload

  def enriched(
    config: Config,
    time: Instant,
    format: GenConfig.Events.Enriched.Format,
    generateEnrichments: Boolean
  ): ScalaGen[List[String]] =
    for {
      identitySource <- getIdentitySource(config)
      events <- SdkEvent.gen(
        config.eventsPerPayload,
        time,
        config.eventsFrequencies,
        config.contextsPerEvent,
        generateEnrichments,
        config.appId,
        identitySource,
        config.duplicates
      )
    } yield events.map { e =>
      format match {
        case GenConfig.Events.Enriched.Format.TSV  => e.toTsv
        case GenConfig.Events.Enriched.Format.JSON => e.toJson(true).noSpaces
      }
    }

  def httpRequest(
    config: Config,
    time: Instant,
    methodFrequencies: Option[GenConfig.Events.Http.MethodFrequencies],
    validEventsOnly: Boolean
  ): ScalaGen[HttpRequest] =
    for {
      identitySource <- getIdentitySource(config)
      request <- HttpRequest.gen(
        config.eventsPerPayload,
        time,
        config.eventsFrequencies,
        config.contextsPerEvent,
        methodFrequencies,
        identitySource,
        config.duplicates,
        validEventsOnly
      )
    } yield request
}

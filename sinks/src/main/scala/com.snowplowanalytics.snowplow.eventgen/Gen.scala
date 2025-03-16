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
    config.duplicates match {
      case Some(dups) =>
        CollectorPayload.genDup(
          dups,
          config.eventsPerPayload,
          time,
          config.eventsFrequencies,
          config.contextsPerEvent
        )
      case None =>
        CollectorPayload.gen(
          config.eventsPerPayload,
          time,
          config.eventsFrequencies,
          config.contextsPerEvent
        )
    }

  def enriched(
    config: Config,
    time: Instant,
    format: GenConfig.Events.Enriched.Format,
    generateEnrichments: Boolean
  ): ScalaGen[List[String]] = {
    val gen = config.duplicates match {
      case Some(dups) =>
        SdkEvent.genDup(
          dups,
          config.eventsPerPayload,
          time,
          config.eventsFrequencies,
          config.contextsPerEvent,
          generateEnrichments,
          config.appId
        )
      case None =>
        SdkEvent.gen(
          config.eventsPerPayload,
          time,
          config.eventsFrequencies,
          config.contextsPerEvent,
          generateEnrichments,
          config.appId
        )
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
    methodFrequencies: Option[GenConfig.Events.Http.MethodFrequencies]
  ): ScalaGen[HttpRequest] =
    config.duplicates match {
      case Some(dups) =>
        HttpRequest.genDup(
          dups,
          config.eventsPerPayload,
          time,
          config.eventsFrequencies,
          config.contextsPerEvent,
          methodFrequencies
        )
      case None =>
        HttpRequest.gen(
          config.eventsPerPayload,
          time,
          config.eventsFrequencies,
          config.contextsPerEvent,
          methodFrequencies
        )
    }
}

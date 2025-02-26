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

import com.snowplowanalytics.snowplow.analytics.scalasdk.Event

import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload
import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent
import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest

object Gen {

  def collectorPayload(config: Config, time: Instant): ScalaGen[CollectorPayload] =
    config.duplicates match {
      case Some(dups) =>
        CollectorPayload.genDup(
          dups.natProb,
          dups.synProb,
          dups.natTotal,
          dups.synTotal,
          config.eventPerPayloadMin,
          config.eventPerPayloadMax,
          time,
          config.eventFrequencies,
          config.contexts
        )
      case None =>
        CollectorPayload.gen(
          config.eventPerPayloadMin,
          config.eventPerPayloadMax,
          time,
          config.eventFrequencies,
          config.contexts
        )
    }

  def enriched(config: Config, time: Instant): ScalaGen[List[Event]] =
    config.duplicates match {
      case Some(dups) =>
        SdkEvent.genDup(
          dups.natProb,
          dups.synProb,
          dups.natTotal,
          dups.synTotal,
          config.eventPerPayloadMin,
          config.eventPerPayloadMax,
          time,
          config.eventFrequencies,
          config.contexts,
          config.generateEnrichments,
          config.fixedAppId
        )
      case None =>
        SdkEvent.gen(
          config.eventPerPayloadMin,
          config.eventPerPayloadMax,
          time,
          config.eventFrequencies,
          config.contexts,
          config.generateEnrichments,
          config.fixedAppId
        )
    }

  def httpRequest(config: Config, time: Instant): ScalaGen[HttpRequest] =
    config.duplicates match {
      case Some(dups) =>
        HttpRequest.genDup(
          dups.natProb,
          dups.synProb,
          dups.natTotal,
          dups.synTotal,
          config.eventPerPayloadMin,
          config.eventPerPayloadMax,
          time,
          config.eventFrequencies,
          config.contexts,
          config.methodFrequencies
        )
      case None =>
        HttpRequest.gen(
          config.eventPerPayloadMin,
          config.eventPerPayloadMax,
          time,
          config.eventFrequencies,
          config.contexts,
          config.methodFrequencies
        )
    }
}

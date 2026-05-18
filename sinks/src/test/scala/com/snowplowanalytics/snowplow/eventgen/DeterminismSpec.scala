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

import fs2.Stream

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import org.specs2.mutable.Specification

class DeterminismSpec extends Specification {
  sequential

  private val fixedTimestamp = Instant.parse("2026-01-01T00:00:00Z")
  private val eventCount     = 50

  private def mkConfig(seed: Option[Long], events: GenConfig.Events): Config =
    Config(
      events = events,
      output = Config.Output.Stdout,
      eventsTotal = Some(eventCount.toLong),
      timestamp = Config.Timestamp.Fixed(fixedTimestamp),
      seed = seed,
      eventsPerPayload = GenConfig.EventsPerPayload(1, 1),
      eventsFrequencies = GenConfig.EventsFrequencies(1, 1, 1, 1, 0, 0, 1, Map.empty),
      contextsPerEvent = GenConfig.ContextsPerEvent(0, 3),
      duplicates = None,
      rate = None,
      userGraph = None,
      appProfiles = None
    )

  private def collectCollectorPayloads(config: Config): List[List[Byte]] =
    Main
      .mkStream[IO, collector.CollectorPayload](config, Gen.collectorPayload(config, _))
      .map(_.toRaw.toList)
      .compile
      .toList
      .unsafeRunSync()

  private def collectEnriched(config: Config): List[String] = {
    val enriched = config.events.asInstanceOf[GenConfig.Events.Enriched]
    Main
      .mkStream[IO, List[String]](config, Gen.enriched(config, _, enriched.format, enriched.generateEnrichments))
      .flatMap(Stream.emits)
      .compile
      .toList
      .unsafeRunSync()
  }

  private def collectHttp(config: Config): List[String] = {
    val http = config.events.asInstanceOf[GenConfig.Events.Http]
    Main
      .mkStream[IO, tracker.HttpRequest](config, Gen.httpRequest(config, _, http.methodFrequencies))
      .map(_.toString)
      .compile
      .toList
      .unsafeRunSync()
  }

  "CollectorPayloads determinism" should {

    "produce identical events across two seeded runs" in {
      val config = mkConfig(seed = Some(12345L), GenConfig.Events.CollectorPayloads)
      val run1   = collectCollectorPayloads(config)
      val run2   = collectCollectorPayloads(config)

      run1.size must_== eventCount
      run1 must_== run2
    }

    "produce different events with different seeds" in {
      val run1 = collectCollectorPayloads(mkConfig(seed = Some(12345L), GenConfig.Events.CollectorPayloads))
      val run2 = collectCollectorPayloads(mkConfig(seed = Some(99999L), GenConfig.Events.CollectorPayloads))

      run1.size must_== eventCount
      run2.size must_== eventCount
      run1 must_!= run2
    }

    "produce different events without seed" in {
      val config = mkConfig(seed = None, GenConfig.Events.CollectorPayloads)
      val run1   = collectCollectorPayloads(config)
      val run2   = collectCollectorPayloads(config)

      run1.size must_== eventCount
      run2.size must_== eventCount
      run1 must_!= run2
    }
  }

  "Enriched events determinism" should {

    val enrichedTsv = GenConfig.Events.Enriched(GenConfig.Events.Enriched.Format.TSV, generateEnrichments = false)

    "produce identical events across two seeded runs" in {
      val config = mkConfig(seed = Some(12345L), enrichedTsv)
      val run1   = collectEnriched(config)
      val run2   = collectEnriched(config)

      run1.size must beGreaterThan(0)
      run1 must_== run2
    }

    "produce different events with different seeds" in {
      val run1 = collectEnriched(mkConfig(seed = Some(12345L), enrichedTsv))
      val run2 = collectEnriched(mkConfig(seed = Some(99999L), enrichedTsv))

      run1.size must beGreaterThan(0)
      run2.size must beGreaterThan(0)
      run1 must_!= run2
    }

    "produce different events without seed" in {
      val config = mkConfig(seed = None, enrichedTsv)
      val run1   = collectEnriched(config)
      val run2   = collectEnriched(config)

      run1.size must beGreaterThan(0)
      run2.size must beGreaterThan(0)
      run1 must_!= run2
    }
  }

  "HTTP requests determinism" should {

    val http = GenConfig.Events.Http(methodFrequencies = None)

    "produce identical events across two seeded runs" in {
      val config = mkConfig(seed = Some(12345L), http)
      val run1   = collectHttp(config)
      val run2   = collectHttp(config)

      run1.size must_== eventCount
      run1 must_== run2
    }

    "produce different events with different seeds" in {
      val run1 = collectHttp(mkConfig(seed = Some(12345L), http))
      val run2 = collectHttp(mkConfig(seed = Some(99999L), http))

      run1.size must_== eventCount
      run2.size must_== eventCount
      run1 must_!= run2
    }

    "produce different events without seed" in {
      val config = mkConfig(seed = None, http)
      val run1   = collectHttp(config)
      val run2   = collectHttp(config)

      run1.size must_== eventCount
      run2.size must_== eventCount
      run1 must_!= run2
    }
  }
}

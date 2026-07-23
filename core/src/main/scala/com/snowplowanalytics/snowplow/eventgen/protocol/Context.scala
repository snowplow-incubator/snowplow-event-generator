/*
 * Copyright (c) 2021-2022 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.eventgen.protocol

import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.snowplow.analytics.scalasdk.SnowplowEvent.Contexts
import com.snowplowanalytics.snowplow.eventgen.primitives._
import com.snowplowanalytics.snowplow.eventgen.protocol.contexts.AllContexts
import com.snowplowanalytics.snowplow.eventgen.GenConfig
import io.circe.syntax.EncoderOps
import io.circe.Json
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen
import java.time.Instant

object Context {

  final case class ContextsWrapper(value: List[SelfDescribingData[Json]]) extends Protocol {
    override def toProto: List[BasicNameValuePair] = value match {
      case Nil  => asKV("cx", None)
      case some => asKV("cx", Some(base64Encode(Contexts(some).asJson)))
    }

    def forSdkEvent: Contexts = Contexts(value)
  }

  private def anyContext(
    alternatives: List[SelfDescribingJsonGen],
    now: Instant,
    config: GenConfig.ContextsPerEvent
  ): Gen[SelfDescribingData[Json]] = {
    val weighted = alternatives.flatMap { ctx =>
      val freq = config.contextFrequencies.getOrElse(ctx.schemaKey.name, config.contextFrequencyDefault)
      if (freq > 0) Some((freq, ctx.gen(now))) else None
    }
    if (weighted.nonEmpty)
      Gen.frequency(weighted: _*)
    else
      throw new IllegalStateException("All context frequencies are 0")
  }

  object ContextsWrapper {

    def gen(now: Instant, contextsPerEvent: GenConfig.ContextsPerEvent): Gen[ContextsWrapper] =
      Gen
        .chooseNum(contextsPerEvent.min, contextsPerEvent.max)
        .flatMap { numContexts =>
          Gen.listOfN(numContexts, anyContext(AllContexts.sentContexts, now, contextsPerEvent))
        }
        .map(ContextsWrapper(_))
  }

  final case class DerivedContextsWrapper(value: List[SelfDescribingData[Json]]) {
    def forSdkEvent: Contexts = Contexts(value)
  }

  object DerivedContextsWrapper {
    def gen(now: Instant, contextsPerEvent: GenConfig.ContextsPerEvent): Gen[DerivedContextsWrapper] =
      Gen
        .chooseNum(0, AllContexts.derivedContexts.length)
        .flatMap { numContexts =>
          Gen.listOfN(numContexts, anyContext(AllContexts.derivedContexts, now, contextsPerEvent))
        }
        .map(DerivedContextsWrapper(_))
  }

}

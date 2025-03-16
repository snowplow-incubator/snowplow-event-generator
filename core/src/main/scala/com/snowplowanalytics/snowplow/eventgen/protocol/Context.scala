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

  private def anyContext(alternatives: List[SelfDescribingJsonGen], now: Instant): Gen[SelfDescribingData[Json]] =
    alternatives match {
      case g1 :: g2 :: grest =>
        Gen.oneOf(g1.gen(now), g2.gen(now), grest.map(_.gen(now)): _*)
      case _ =>
        // should never happen, because our controlled lists have >2 generators
        throw new IllegalStateException("Got invalid number of generators")
    }

  object ContextsWrapper {

    def gen(now: Instant, contextsPerEvent: GenConfig.ContextsPerEvent): Gen[ContextsWrapper] =
      Gen
        .chooseNum(contextsPerEvent.min, contextsPerEvent.max)
        .flatMap { numContexts =>
          Gen.listOfN(numContexts, anyContext(AllContexts.sentContexts, now))
        }
        .map(ContextsWrapper(_))
  }

  final case class DerivedContextsWrapper(value: List[SelfDescribingData[Json]]) {
    def forSdkEvent: Contexts = Contexts(value)
  }

  object DerivedContextsWrapper {
    def gen(now: Instant): Gen[DerivedContextsWrapper] =
      Gen
        .chooseNum(0, AllContexts.derivedContexts.length)
        .flatMap { numContexts =>
          Gen.listOfN(numContexts, anyContext(AllContexts.derivedContexts, now))
        }
        .map(DerivedContextsWrapper(_))
  }

}

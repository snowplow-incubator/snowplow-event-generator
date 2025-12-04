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

import com.snowplowanalytics.snowplow.eventgen.protocol.Context.{ContextsWrapper, DerivedContextsWrapper}
import com.snowplowanalytics.snowplow.eventgen.protocol.event._
import com.snowplowanalytics.snowplow.eventgen.protocol.common._
import com.snowplowanalytics.snowplow.eventgen.protocol.contexts.EcommerceUser
import com.snowplowanalytics.snowplow.eventgen.{CorrelatedUser, GenConfig}
import io.circe.Json
import io.circe.syntax._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen
import org.scalacheck.rng.Seed

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import scala.util.Random

final case class Body(
  e: EventType,
  app: Application,
  dt: Option[DateTime],
  dev: Option[Device],
  tv: TrackerVersion,
  et: EventTransaction,
  u: Option[User],
  event: BodyEvent,
  contexts: ContextsWrapper,
  derivedContexts: DerivedContextsWrapper
) extends Protocol {
  override def toProto: List[BasicNameValuePair] =
    asKV("e", Some(e)) ++
      event.toProto ++
      app.toProto ++
      et.toProto ++
      dt.fold(List.empty[BasicNameValuePair])(_.toProto) ++
      dev.fold(List.empty[BasicNameValuePair])(_.toProto) ++
      tv.toProto ++
      u.fold(List.empty[BasicNameValuePair])(_.toProto) ++
      contexts.toProto

  def toPayloadElement: Json =
    toProto.foldLeft(Map.empty[String, String])((acc, kv) => acc ++ Map(kv.getName -> kv.getValue)).asJson
}

object Body {

  private val DuplicationSeed      = 20000L
  private val ProbabilityPrecision = 10000

  lazy val dupRng = new Random(DuplicationSeed)

  def gen(
    time: Instant,
    frequencies: GenConfig.EventsFrequencies,
    contexts: GenConfig.ContextsPerEvent,
    identitySource: GenConfig.IdentitySource,
    duplicates: Option[GenConfig.Duplicates]
  ): Gen[Body] = {
    val etGen = duplicates match {
      case Some(dups) => EventTransaction.genDup(dups.synProb, dups.synTotal)
      case None       => EventTransaction.gen
    }
    val baseGen = genWithEt(etGen, time, frequencies, contexts, identitySource)
    duplicates match {
      case Some(dups) => withNaturalDuplicates(baseGen, dups)
      case None       => baseGen
    }
  }

  private def withNaturalDuplicates(baseGen: Gen[Body], duplicates: GenConfig.Duplicates): Gen[Body] =
    baseGen.withPerturb { in =>
      if (duplicates.natProb == 0f | duplicates.natTotal == 0)
        in
      else if (dupRng.nextInt(ProbabilityPrecision) < (duplicates.natProb * ProbabilityPrecision))
        Seed(dupRng.nextInt(duplicates.natTotal).toLong)
      else
        in
    }

  private def genWithEt(
    etGen: Gen[EventTransaction],
    time: Instant,
    frequencies: GenConfig.EventsFrequencies,
    contexts: GenConfig.ContextsPerEvent,
    identitySource: GenConfig.IdentitySource
  ) =
    for {
      e <- EventType.gen(frequencies)
      app <- identitySource match {
        case GenConfig.IdentitySource.ProfileGraph(appId, _) => Application.genWithAppId(appId)
        case _                                               => Application.gen
      }
      et  <- etGen
      dt  <- DateTime.genOpt(time)
      dev <- Device.genOpt
      tv  <- TrackerVersion.gen

      correlatedData <- identitySource match {
        case GenConfig.IdentitySource.ProfileGraph(appId, config) =>
          CorrelatedUser.gen(config, Some(appId)).map(Some(_))
        case GenConfig.IdentitySource.SingleGraph(config) =>
          CorrelatedUser.gen(config, None).map(Some(_))
        case GenConfig.IdentitySource.NoIdentity =>
          Gen.const(None)
      }

      u <- correlatedData match {
        case Some(data) => Gen.const(Some(data.user.copy(uid = data.authenticatedUserId)))
        case None       => User.genOpt
      }

      event <- e match {
        case EventType.Struct          => StructEvent.gen
        case EventType.Unstruct        => UnstructEventWrapper.gen(time, frequencies)
        case EventType.PageView        => PageView.gen
        case EventType.PagePing        => PagePing.gen
        case EventType.Transaction     => TransactionEvent.gen
        case EventType.TransactionItem => TransactionItemEvent.gen
      }

      regularContexts <- Context.ContextsWrapper.gen(time, contexts)
      authUserContext <- correlatedData match {
        case Some(data) => EcommerceUser.genWithUserId(data.authenticatedUserId)
        case None       => Gen.const(None)
      }

      allContexts = authUserContext.toList ++ regularContexts.value
      derivedContexts <- Context.DerivedContextsWrapper.gen(time)
    } yield Body(e, app, dt, dev, tv, et, u, event, ContextsWrapper(allContexts), derivedContexts)

  def encodeValue(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString)
}

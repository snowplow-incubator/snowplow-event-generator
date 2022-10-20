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

import com.snowplowanalytics.snowplow.eventgen.protocol.Context.ContextsWrapper
import com.snowplowanalytics.snowplow.eventgen.protocol.event._
import com.snowplowanalytics.snowplow.eventgen.protocol.common._
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
                       context: Option[ContextsWrapper]
                     ) extends Protocol {
  override def toProto: List[BasicNameValuePair] = {
    asKV("e", Some(e)) ++
      event.toProto ++
      app.toProto ++
      et.toProto ++
      dt.fold(List.empty[BasicNameValuePair])(_.toProto) ++
      dev.fold(List.empty[BasicNameValuePair])(_.toProto) ++
      tv.toProto ++
      u.fold(List.empty[BasicNameValuePair])(_.toProto) ++
      context.fold(List.empty[BasicNameValuePair])(_.toProto)
  }

  def toPayloadElement: Json = toProto.foldLeft(Map.empty[String, String])(
    (acc, kv) => acc ++ Map(kv.getName -> kv.getValue)
  ).asJson
}

object Body {

  lazy val dupRng = new Random(20000L)

  def genDup(natProb: Float, synProb: Float, natTotal: Int, synTotal: Int, now: Instant): Gen[Body] =
    genWithEt(EventTransaction.genDup(synProb, synTotal), now).withPerturb(in =>
      if (natProb == 0F | natTotal == 0)
        in
      else if (dupRng.nextInt(10000) < (natProb * 10000))
        Seed(dupRng.nextInt(natTotal).toLong)
      else
        in
    )


  private def genWithEt(etGen: Gen[EventTransaction], now: Instant) = for {
    e <- EventType.gen
    app <- Application.gen
    et <- etGen
    dt <- DateTime.genOpt(now)
    dev <- Device.genOpt
    tv <- TrackerVersion.gen
    u <- User.genOpt
    event <- e match {
      case EventType.Struct => StructEvent.gen
      case EventType.Unstruct => UnstructEventWrapper.gen
      case EventType.PageView => PageView.gen
      case EventType.PagePing => PagePing.gen
    }
    context <- Context.ContextsWrapper.genOps
  } yield Body(e, app, dt, dev, tv, et, u, event, context)


  def gen(now: Instant): Gen[Body] = genWithEt(EventTransaction.gen, now)

  def encodeValue(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString)
}


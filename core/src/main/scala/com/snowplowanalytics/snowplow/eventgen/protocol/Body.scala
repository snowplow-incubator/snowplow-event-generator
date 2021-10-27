package com.snowplowanalytics.snowplow.eventgen.protocol

import com.snowplowanalytics.snowplow.eventgen.protocol.Context.ContextsWrapper
import com.snowplowanalytics.snowplow.eventgen.protocol.event._
import com.snowplowanalytics.snowplow.eventgen.protocol.common._
import io.circe.Json
import io.circe.syntax._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen


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
  val gen: Gen[Body] = for {
    e <- EventType.gen
    app <- Application.gen
    et <- EventTransaction.gen
    dt <- DateTime.genOpt
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
}


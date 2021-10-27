package com.snowplowanalytics.snowplow.eventgen.protocol.event

import com.snowplowanalytics.snowplow.eventgen.protocol.event.UnstructEvent.UnstructEventData
import com.snowplowanalytics.snowplow.eventgen.utils.base64Encode
import io.circe.syntax._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen


final case class UnstructEventWrapper(
                                       event: UnstructEventData,
                                       b64: Boolean
                                     ) extends BodyEvent {
  override def toProto: List[BasicNameValuePair] = {
    if (!b64)
      asKV("ue_pr", Some(event.toUnstructEvent.asJson))
    else
      asKV("ue_px", Some(base64Encode(event.toUnstructEvent.asJson)))
  }
}

object UnstructEventWrapper {
  val gen: Gen[UnstructEventWrapper] = UnstructEvent.genLink.map(l => UnstructEventWrapper(l, b64 = true))
}

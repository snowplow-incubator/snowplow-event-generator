package com.snowplowanalytics.snowplow.eventgen.protocol.common

import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen

final case class TrackerVersion(
                                 tv: String // v_tracker
                               ) extends Protocol {
  override def toProto: List[BasicNameValuePair] = asKV("tv", Some(tv))

}

object TrackerVersion {
  def gen: Gen[TrackerVersion] = Gen.oneOf("scala-tracker_1.0.0", "js_2.0.0", "go_1.2.3")
    .map(TrackerVersion.apply)
}
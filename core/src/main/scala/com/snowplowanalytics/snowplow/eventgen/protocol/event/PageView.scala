package com.snowplowanalytics.snowplow.eventgen.protocol.event

import com.snowplowanalytics.snowplow.eventgen.protocol.common.Web
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen

final case class PageView(
                     override val deps: List[Web] // all web props
                   ) extends LegacyEvent {
  override def toProto: List[BasicNameValuePair] = deps.flatMap(_.toProto)
}

object PageView {
  def gen: Gen[PageView] = Gen.listOfN(1, Web.gen).map(PageView.apply)
}
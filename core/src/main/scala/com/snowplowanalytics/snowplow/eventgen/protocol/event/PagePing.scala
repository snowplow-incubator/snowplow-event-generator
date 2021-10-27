package com.snowplowanalytics.snowplow.eventgen.protocol.event

import com.snowplowanalytics.snowplow.eventgen.protocol.common.Web
import org.scalacheck.Gen
import com.snowplowanalytics.snowplow.eventgen.utils._
import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.protocol._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.cats.implicits._

final case class PagePing(
                     override val deps: List[Protocol], // all web props
                     pp_mix: Option[Int], // pp_xoffset_min
                     pp_max: Option[Int], //pp_xoffset_max
                     pp_miy: Option[Int], //pp_yoffset_min
                     pp_may: Option[Int] //pp_yoffset_max
                   ) extends LegacyEvent {


  override def toProto: List[BasicNameValuePair] =
    deps.flatMap(_.toProto) ++
      asKV("pp_mix", pp_mix) ++
      asKV("pp_max", pp_max) ++
      asKV("pp_miy", pp_miy) ++
      asKV("pp_may", pp_may)

}

object PagePing {
  def gen: Gen[PagePing] = (
    Gen.listOfN(1, Web.gen),
    genIntOpt,
    genIntOpt,
    genIntOpt,
    genIntOpt
    ).mapN(PagePing.apply)
}
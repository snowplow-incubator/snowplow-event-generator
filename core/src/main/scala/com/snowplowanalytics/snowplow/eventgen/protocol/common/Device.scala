package com.snowplowanalytics.snowplow.eventgen.protocol.common

import com.snowplowanalytics.snowplow.eventgen.protocol._
import com.snowplowanalytics.snowplow.eventgen.utils.{Dimensions, genDimensions}
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen

final case class Device(
                   res: Option[Dimensions] // dvce_screenheight and dvce_screenwidth
                 ) extends Protocol {
  override def toProto: List[BasicNameValuePair] = asKV("res",res)
}

object Device {
  def gen: Gen[Device] = Gen.option(genDimensions).map(Device.apply)

  def genOpt: Gen[Option[Device]] = Gen.option(gen)
}

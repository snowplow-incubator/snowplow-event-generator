package com.snowplowanalytics.snowplow.eventgen.utils

case class Dimensions(x: Int, y: Int){
  override def toString: String = s"${x}x${y}"
}

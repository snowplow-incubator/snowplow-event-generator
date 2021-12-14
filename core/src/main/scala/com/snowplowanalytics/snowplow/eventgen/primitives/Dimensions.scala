package com.snowplowanalytics.snowplow.eventgen.primitives

case class Dimensions(x: Int, y: Int){
  override def toString: String = s"${x}x${y}"
}

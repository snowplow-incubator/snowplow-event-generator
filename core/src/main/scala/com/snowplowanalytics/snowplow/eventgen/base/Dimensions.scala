package com.snowplowanalytics.snowplow.eventgen.base

case class Dimensions(x: Int, y: Int){
  override def toString: String = s"${x}x${y}"
}

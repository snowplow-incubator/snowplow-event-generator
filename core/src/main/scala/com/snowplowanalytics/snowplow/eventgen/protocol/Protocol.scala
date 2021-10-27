package com.snowplowanalytics.snowplow.eventgen.protocol

import org.apache.http.message.BasicNameValuePair

import java.time.Instant

trait Protocol {
  def toProto: List[BasicNameValuePair]

  def deps: List[Protocol] = List.empty[Protocol]

  private def mkString[T](v: T): String = {
    v match {
      case v: Boolean => if (v) "1" else "0"
      case v: Instant => v.toEpochMilli.toString
      case _ => v.toString
    }
  }

  def asKV[V](k: String, ov: Option[V]): List[BasicNameValuePair] =
    ov.fold(List.empty[BasicNameValuePair])(el => List(new BasicNameValuePair(k, mkString(el))))

}
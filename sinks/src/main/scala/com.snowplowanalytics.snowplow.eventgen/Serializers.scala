package com.snowplowanalytics.snowplow.eventgen

import cats.effect.Sync
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import fs2.{Pipe, Stream}
import fs2.compression.Compression
import fs2.text.{base64, utf8}

object Serializers {

  def stringSerializer[F[_]: Sync](compress: Boolean): Pipe[F, String, Byte] = {
    val pipe: Pipe[F, String, Byte] = _.flatMap(x => Stream(x, "\n")).through(utf8.encode)
    if (compress)
      pipe.andThen(Compression[F].gzip())
    else
      pipe
  }

  def rawSerializer[F[_] : Sync](compress: Boolean = true): Pipe[F, collector.CollectorPayload, Byte] =
    _.flatMap(e => Stream.emits(e.toRaw))
      .through(base64.encode)
      .through(stringSerializer(compress))

  def enrichedTsvSerializer[F[_] : Sync](compress: Boolean = true): Pipe[F, Event, Byte] =
    _.map(_.toTsv).through(stringSerializer(compress))

  def enrichedJsonSerializer[F[_] : Sync](compress: Boolean = true): Pipe[F, Event, Byte] =
    _.map(_.toJson(false).noSpaces).through(stringSerializer(compress))
}

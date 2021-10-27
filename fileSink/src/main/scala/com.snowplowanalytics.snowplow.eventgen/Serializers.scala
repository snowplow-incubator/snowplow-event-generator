package com.snowplowanalytics.snowplow.eventgen

import cats.effect.Sync
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.eventgen.Config.EnrichFormat
import fs2.{Pipe, Stream}
import fs2.compression.Compression
import fs2.text.{base64, utf8}

object Serializers {
  def rawSerializer[F[_] : Sync]: Pipe[F, collector.CollectorPayload, Byte] = (in: Stream[F, collector.CollectorPayload]) => {
    in.flatMap(e => Stream.emits(e.toRaw))
      .through(base64.encode)
      .intersperse("\n")
      .through(utf8.encode)
      .through(Compression[F].gzip())
  }

  def enrichedSerializer[F[_] : Sync](config: Config): Pipe[F, Event, Byte] = (in: Stream[F, Event]) =>
    in.map(e => config.enrichFormat match {
      case EnrichFormat.Json => e.toJson(false).noSpaces
      case EnrichFormat.Tsv => e.toTsv
    })
      .intersperse("\n")
      .through(utf8.encode)
      .through(Compression[F].gzip())
}
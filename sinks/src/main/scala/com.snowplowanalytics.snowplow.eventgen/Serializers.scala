package com.snowplowanalytics.snowplow.eventgen

import cats.effect.Sync
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.eventgen.Config.EnrichFormat
import fs2.{Pipe, Stream}
import fs2.compression.Compression
import fs2.text.{base64, utf8}

object Serializers {
  def rawSerializer[F[_] : Sync](compress: Boolean = true): Pipe[F, collector.CollectorPayload, Byte] = (in: Stream[F, collector.CollectorPayload]) => {
    val st = in.flatMap(e => Stream.emits(e.toRaw))
      .through(base64.encode)
      .intersperse("\n")
      .through(utf8.encode)
    if (compress)
      st.through(Compression[F].gzip())
    else
      st
  }

  def enrichedSerializer[F[_] : Sync](fmt: EnrichFormat, compress: Boolean = true): Pipe[F, Event, Byte] = (in: Stream[F, Event]) => {
    val st = in.map((e: Event) => fmt match {
      case EnrichFormat.Json => e.toJson(false).noSpaces
      case EnrichFormat.Tsv => e.toTsv
    })
      .flatMap(x => Stream(x, "\n"))
      .through(utf8.encode)

    if (compress)
      st.through(Compression[F].gzip())
    else
      st
  }
}

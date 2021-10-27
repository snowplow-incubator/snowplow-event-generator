package com.snowplowanalytics.snowplow.eventgen

import cats.effect.Sync
import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent
import fs2.Stream
import cats.implicits._
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import org.scalacheck.Gen
import org.scalacheck.rng.Seed

case class EventStream(seed: Long) {

  val rng = new scala.util.Random(seed)

  private def runGen[F[_] : Sync, A](gen: Gen[A]): F[A] = {
    def go(attempt: Int): F[A] =
      if (attempt >= 5) Sync[F].raiseError(new RuntimeException("Couldn't generate a pair after several attempts"))
      else Sync[F].delay(gen.apply(Gen.Parameters.default, Seed(rng.nextLong()))).flatMap {
        case Some(a) => Sync[F].pure(a)
        case None => go(attempt + 1)
      }

    go(1)
  }

  def makeStreamPair[F[_] : Sync]: Stream[F, (collector.CollectorPayload, List[Event])] =
    Stream.repeatEval(runGen(SdkEvent.genPair))

}

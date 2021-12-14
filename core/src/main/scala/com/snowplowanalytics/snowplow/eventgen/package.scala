package com.snowplowanalytics.snowplow

import org.scalacheck.Gen
import org.scalacheck.rng.Seed

import scala.annotation.tailrec
import scala.util.Random

package object eventgen {

//  def runWithDups()

  def runGen[F[_], A](gen: Gen[A], seed: Random): A = {

    @tailrec
    def go(attempt: Int): A =
      if (attempt >= 5)
        throw new RuntimeException("Couldn't generate a pair after several attempts")
      else
        gen.apply(Gen.Parameters.default, Seed(seed.nextLong())) match {
          case Some(a) => a
          case None => go(attempt + 1)
        }

    go(1)
  }
}

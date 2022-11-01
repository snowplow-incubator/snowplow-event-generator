/*
 * Copyright (c) 2021-2022 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
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
          case None    => go(attempt + 1)
        }

    go(1)
  }
}

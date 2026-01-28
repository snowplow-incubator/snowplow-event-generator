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
package com.snowplowanalytics.snowplow.eventgen.primitives

import org.scalacheck.Gen

sealed trait IpAddress {
  def repr: String

  override def toString: String = repr
}

object IpAddress {
  case class IpAddressV4(repr: String) extends IpAddress

  object IpAddressV4 {
    def gen: Gen[IpAddressV4] =
      for {
        a <- Gen.chooseNum(0, 255)
        b <- Gen.chooseNum(0, 255)
        c <- Gen.chooseNum(0, 255)
        d <- Gen.chooseNum(0, 255)
      } yield IpAddressV4(s"$a.$b.$c.$d")
  }

  case class IpAddressV6(repr: String) extends IpAddress

  object IpAddressV6 {
    private val hexSegment: Gen[Int] = Gen.chooseNum(0, 65535)

    def gen: Gen[IpAddressV6] =
      for {
        a <- hexSegment
        b <- hexSegment
        c <- hexSegment
        d <- hexSegment
        e <- hexSegment
        f <- hexSegment
        g <- hexSegment
        h <- hexSegment
      } yield IpAddressV6(f"$a%x:$b%x:$c%x:$d%x:$e%x:$f%x:$g%x:$h%x")
  }

  val gen: Gen[IpAddress] = Gen.oneOf(IpAddressV4.gen, IpAddressV6.gen)

  val genOpt = Gen.option(gen)
}

package com.snowplowanalytics.snowplow.eventgen

import io.circe.Json
import org.scalacheck.{Arbitrary, Gen}

import java.nio.charset.Charset
import java.time.Instant
import java.util.{Base64, TimeZone}
import scala.util.Random

package object utils {
  private val base64Encoder = Base64.getEncoder
  private val now = System.currentTimeMillis().toInt

  type Epoch = Int

  def genBool: Gen[Boolean] = Arbitrary.arbBool.arbitrary

  def genBoolOpt: Gen[Option[Boolean]] = Gen.option(genBool)

  def genInt: Gen[Int] = Gen.chooseNum(1, 1000000)

  def genIntOpt: Gen[Option[Int]] = Gen.option(genInt)

  def genDblOpt: Gen[Option[Double]] = Gen.option(Arbitrary.arbDouble.arbitrary)

  def genLocaleStr: Gen[String] = Arbitrary.arbInt.arbitrary.map(_.toString)

  def genLocaleStrOpt: Gen[Option[String]] = Gen.option(genLocaleStr)

  def genWords: Gen[String] = Gen.chooseNum(1, 10).map(n => Random.shuffle(LoremIpsum.take(n)).mkString(" ").capitalize)

  def genWordsOpt: Gen[Option[String]] = Gen.option(genWords)

  def genString(prefix: String, len: Int): Gen[String] = Gen.stringOfN(len, Gen.alphaNumChar)
    .map(s => s"${prefix}_$s")

  def genStringOpt(prefix: String, len: Int): Gen[Option[String]] = Gen.option(genString(prefix, len))

  def strGen(len: Int, g: Gen[Char]): Gen[String] = Gen.chooseNum(1, len).flatMap { x => Gen.stringOfN(x, g) }

  def genInstant: Gen[Epoch] = Gen.chooseNum(now - 100000, now)

  def genInstantOpt: Gen[Option[Instant]] = Gen.option(genInstant.map(s => Instant.ofEpochSecond(s.toLong)))

  def genTz: Gen[String] = Gen.oneOf(TimeZone.getAvailableIDs.toSeq)

  def genTzOpt: Gen[Option[String]] = Gen.option(genTz)

  val genIp: Gen[String] = IpAddress.gen.map(_.repr)

  val genIpOpt: Gen[Option[String]] = Gen.option(genIp)

  def genDimensions: Gen[Dimensions] = for {
    x <- Gen.chooseNum(1, 10000)
    y <- Gen.chooseNum(1, 10000)
  } yield Dimensions(x, y)

  def genDimensionsOpt: Gen[Option[Dimensions]] = Gen.option(genDimensions)

  def genUserAgent: Gen[String] = Gen.oneOf(
    "Mozilla/5.0 (iPad; CPU OS 6_1_3 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10B329 Safari/8536.25",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
    "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1",
    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1",
    "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; Trident/5.0)",
    "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
  )

  def genUserAgentOpt: Gen[Option[String]] = Gen.option(genUserAgent)

  def genCharsetStr: Gen[String] = Gen.oneOf(Charset.availableCharsets().keySet().toArray.toSeq.map(_.toString))

  def genCharsetStrOpt: Gen[Option[String]] = Gen.option(genCharsetStr)

  private val LoremIpsum: Seq[String] =
    """Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
      | Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure
      | dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
      | proident, sunt in culpa qui officia deserunt mollit anim id est laborum.""".stripMargin
      .replaceAll("""[\p{Punct}]""", "")
      .toLowerCase
      .split(" ")
      .toList

  def base64Encode(j: Json): String = base64Encoder.encodeToString(j.noSpaces.getBytes)
}

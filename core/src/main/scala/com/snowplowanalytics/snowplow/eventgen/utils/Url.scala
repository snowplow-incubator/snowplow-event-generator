package com.snowplowanalytics.snowplow.eventgen.utils

import org.scalacheck.Gen

import cats.implicits._
import org.scalacheck.cats.implicits._

case class Url(
                scheme: String,
                prefix: String,
                domain: String,
                port: Option[Int],
                tld: String,
                path: String
              ) {
  override def toString: String = port match {
    case Some(port) => s"""$scheme://$prefix$domain$tld:$port$path"""
    case None => s"""$scheme://$prefix$domain$tld$path"""
  }

  def sdkPort: Int = port match {
    case Some(value) => value
    case None => scheme match {
      case "https" => 443
      case _ => 80
    }
  }

  def host: String = s"$prefix$domain$tld"
}

object Url {

  def gen: Gen[Url] = (urlSchemeGen, urlPrefixGen, urlDomainGen, urlPortGen, urlTldGen, urlPathGen).mapN(Url.apply)

  def genOpt: Gen[Option[Url]] = Gen.option((urlSchemeGen, urlPrefixGen, urlDomainGen, urlPortGen, urlTldGen, urlPathGen).mapN(Url.apply))

  private val urlSchemeGen: Gen[String] = Gen.oneOf("http", "https")
  private val urlPrefixGen: Gen[String] = Gen.oneOf("", "www.")
  private val urlDomainGen: Gen[String] = Gen.stringOfN(7, Gen.alphaNumChar)
  private val urlTldGen: Gen[String] = Gen.oneOf(".com", ".net", ".co.uk", ".bg", ".ru", ".fr", ".tr")
  private val urlPortGen: Gen[Option[Int]] = Gen.option(Gen.chooseNum(1, 65335))
  private val urlPathGen: Gen[String] = Gen.stringOfN(15, Gen.alphaNumChar).map(s => s"/$s")
}
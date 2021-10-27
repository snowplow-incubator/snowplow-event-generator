package com.snowplowanalytics.snowplow.eventgen.protocol.common

import cats.implicits._
import com.snowplowanalytics.snowplow.eventgen.protocol._
import com.snowplowanalytics.snowplow.eventgen.base._
import org.apache.http.message.BasicNameValuePair
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._

case class Web(
                url: Option[Url] = None, // page_url                                // Note we may override this below
                ua: Option[String] = None, // useragent
                page: Option[String] = None, // page_title
                refr: Option[Url] = None, // page_referrer
                fp: Option[Int] = None, // user_fingerprint
                cookie: Option[Boolean] = None, // br_cookies
                lang: Option[String] = None, // br_lang
                f_pdf: Option[Boolean] = None, // br_features_pdf
                f_qt: Option[Boolean] = None, // br_features_quicktime
                f_realp: Option[Boolean] = None, // br_features_realplayer
                f_wma: Option[Boolean] = None, // br_features_windowsmedia
                f_dir: Option[Boolean] = None, // br_features_director
                f_fla: Option[Boolean] = None, // br_features_flash
                f_java: Option[Boolean] = None, // br_features_java
                f_gears: Option[Boolean] = None, // br_features_gears
                f_ag: Option[Boolean] = None, // br_features_silverlight
                cd: Option[Int] = None, // br_colordepth
                ds: Option[Dimensions] = None, // doc_width and doc_height                // Tuple
                cs: Option[String] = None, // doc_charset
                vp: Option[Dimensions] = None // br_viewwidth and br_viewheight          // Tuple
              ) extends Protocol {
  override def toProto: List[BasicNameValuePair] =
    asKV("url", url) ++
      asKV("ua", ua) ++
      asKV("page", page) ++
      asKV("refr", refr) ++
      asKV("fp", fp) ++
      asKV("cookie", cookie) ++
      asKV("lang", lang) ++
      asKV("f_pdf", f_pdf) ++
      asKV("f_qt", f_qt) ++
      asKV("f_realp", f_realp) ++
      asKV("f_wma", f_wma) ++
      asKV("f_dir", f_dir) ++
      asKV("f_fla", f_fla) ++
      asKV("f_java", f_java) ++
      asKV("f_gears", f_gears) ++
      asKV("f_ag", f_ag) ++
      asKV("cd", cd) ++
      asKV("ds", ds) ++
      asKV("cs", cs) ++
      asKV("vp", vp)
}

object Web {

  def gen: Gen[Web] = (
    Url.genOpt,
    genUserAgentOpt,
    genWordsOpt,
    Url.genOpt,
    genIntOpt,
    genBoolOpt,
    genLocaleStrOpt,
    genBoolOpt,
    genBoolOpt,
    genBoolOpt,
    genBoolOpt,
    genBoolOpt,
    genBoolOpt,
    genBoolOpt,
    genBoolOpt,
    genBoolOpt,
    genIntOpt,
    genDimensionsOpt,
    genCharsetStrOpt,
    genDimensionsOpt
    ).mapN(Web.apply)

  def genOpt: Gen[Option[Web]] = Gen.option(gen)

}

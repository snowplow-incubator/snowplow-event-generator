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
package com.snowplowanalytics.snowplow.eventgen.protocol.contexts

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer}
import com.snowplowanalytics.snowplow.eventgen.protocol.SelfDescribingJsonGen
import com.snowplowanalytics.snowplow.eventgen.protocol.implicits._
import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.scalacheck.Gen
import io.circe.Json
import java.time.Instant

object YauaaContext extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("nl.basjes", "yauaa_context", "jsonschema", SchemaVer.Full(1, 0, 4))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "deviceClass"                     -> deviceClassGen.required,
      "deviceName"                      -> strGen(1, 128).optional,
      "deviceBrand"                     -> strGen(1, 128).optional,
      "deviceCpu"                       -> strGen(1, 128).optional,
      "deviceCpuBits"                   -> strGen(1, 128).optional,
      "deviceFirmwareVersion"           -> strGen(1, 128).optional,
      "deviceVersion"                   -> strGen(1, 128).optional,
      "operatingSystemClass"            -> operatingSystemClassGen.optional,
      "operatingSystemName"             -> strGen(1, 128).optional,
      "operatingSystemVersion"          -> strGen(1, 128).optional,
      "operatingSystemNameVersion"      -> strGen(1, 128).optional,
      "operatingSystemVersionBuild"     -> strGen(1, 128).optional,
      "layoutEngineClass"               -> layoutEngineClassGen.optional,
      "layoutEngineName"                -> strGen(1, 128).optional,
      "layoutEngineVersion"             -> strGen(1, 128).optional,
      "layoutEngineVersionMajor"        -> strGen(1, 128).optional,
      "layoutEngineNameVersionMajor"    -> strGen(1, 128).optional,
      "layoutEngineBuild"               -> strGen(1, 128).optional,
      "agentClass"                      -> agentClassGen.optional,
      "agentName"                       -> strGen(1, 128).optional,
      "agentVersion"                    -> strGen(1, 128).optional,
      "agentVersionMajor"               -> strGen(1, 128).optional,
      "agentNameVersion"                -> strGen(1, 128).optional,
      "agentBuild"                      -> strGen(1, 128).optional,
      "agentLanguage"                   -> strGen(1, 128).optional,
      "agentLanguageCode"               -> strGen(1, 128).optional,
      "agentInformationEmail"           -> strGen(1, 128).optional,
      "agentInformationUrl"             -> strGen(1, 128).optional,
      "agentSecurity"                   -> agentSecurityGen.optional,
      "agentUuid"                       -> Gen.uuid.optional,
      "webviewAppName"                  -> strGen(1, 128).optional,
      "webviewAppVersion"               -> strGen(1, 128).optional,
      "webviewAppVersionMajor"          -> strGen(1, 128).optional,
      "webviewAppNameVersionMajor"      -> strGen(1, 128).optional,
      "facebookCarrier"                 -> strGen(1, 128).optional,
      "facebookDeviceClass"             -> strGen(1, 128).optional,
      "facebookDeviceName"              -> strGen(1, 128).optional,
      "facebookDeviceVersion"           -> strGen(1, 128).optional,
      "facebookFBOP"                    -> strGen(1, 128).optional,
      "facebookFBSS"                    -> strGen(1, 128).optional,
      "facebookOperatingSystemName"     -> strGen(1, 128).optional,
      "facebookOperatingSystemVersion"  -> strGen(1, 128).optional,
      "anonymized"                      -> strGen(1, 128).optional,
      "hackerAttackVector"              -> strGen(1, 128).optional,
      "hackerToolkit"                   -> strGen(1, 128).optional,
      "koboAffiliate"                   -> strGen(1, 128).optional,
      "koboPlatformId"                  -> strGen(1, 128).optional,
      "IECompatibilityVersion"          -> strGen(1, 128).optional,
      "IECompatibilityVersionMajor"     -> strGen(1, 128).optional,
      "IECompatibilityNameVersion"      -> strGen(1, 128).optional,
      "IECompatibilityNameVersionMajor" -> strGen(1, 128).optional,
      "carrier"                         -> strGen(1, 128).optional,
      "gSAInstallationID"               -> strGen(1, 128).optional,
      "networkType"                     -> strGen(1, 128).optional,
      "operatingSystemNameVersionMajor" -> strGen(1, 128).optional,
      "operatingSystemVersionMajor"     -> strGen(1, 128).optional
    )

  private def deviceClassGen: Gen[String] =
    Gen.oneOf(
      "Desktop",
      "Anonymized",
      "Unknown",
      "Mobile",
      "Tablet",
      "Phone"
    )

  private def operatingSystemClassGen: Gen[String] =
    Gen.oneOf(
      "Desktop",
      "Mobile",
      "Cloud",
      "Embedded"
    )

  private def layoutEngineClassGen: Gen[String] =
    Gen.oneOf(
      "Browser",
      "Mobile App",
      "Unknown"
    )

  private def agentClassGen: Gen[String] =
    Gen.oneOf(
      "Browser",
      "Browser Webview",
      "Mobile App",
      "Robot",
      "Unknown"
    )

  private def agentSecurityGen: Gen[String] =
    Gen.oneOf(
      "Weak security",
      "Strong security",
      "Unknown"
    )

}

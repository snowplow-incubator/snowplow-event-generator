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

object JavaContext extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.amazon.aws.lambda", "java_context", "jsonschema", SchemaVer.Full(1, 0, 0))

  val customGen: Gen[Json] =
    Map(
      "abc" -> strGen(1, 20).optional,
      "xyz" -> strGen(1, 20).optional
    ).genObject

  val clientGen: Gen[Json] =
    Map(
      "appTitle"       -> strGen(1, 20).optional,
      "appVersionName" -> strGen(1, 20).optional,
      "appVersionCode" -> strGen(1, 20).optional,
      "appPackageName" -> strGen(1, 20).optional
    ).genObject

  val genClientContextGen: Gen[Json] =
    Map(
      "client" -> clientGen.optional,
      "custom" -> customGen.optional
    ).genObject

  val identityGen: Gen[Json] =
    Map(
      "identityId"     -> strGen(1, 20).optional,
      "identityPoolId" -> strGen(1, 20).optional
    ).genObject

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "functionName"        -> strGen(1, 20).optional,
      "logStreamName"       -> strGen(1, 20).optional,
      "awsRequestId"        -> strGen(1, 20).optional,
      "remainingTimeMillis" -> Gen.chooseNum(0, 3600).optional,
      "logGroupName"        -> strGen(1, 20).optional,
      "memoryLimitInMB"     -> Gen.chooseNum(0, 10000).optional,
      "clientContext"       -> genClientContextGen.optional,
      "identity"            -> identityGen.optional
    )
}

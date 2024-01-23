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

object InstanceIdentityDocument extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.amazon.aws.ec2", "instance_identity_document", "jsonschema", SchemaVer.Full(1, 0, 0))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "instanceId"         -> strGen(10, 21).required,
      "devpayProductCodes" -> Gen.listOfN(5, strGen(1, 5)).optionalOrNull,
      "billingProducts"    -> Gen.listOfN(5, strGen(1, 5)).optionalOrNull,
      "availabilityZone"   -> strGen(1, 20).optional,
      "accountId"          -> strGen(1, 20).optional,
      "ramdiskId"          -> strGen(12, 21).optionalOrNull,
      "architecture"       -> strGen(1, 20).optional,
      "instanceType"       -> strGen(1, 20).optional,
      "version"            -> strGen(1, 20).optional,
      "pendingTime"        -> genInstant(now).map(_.toString).optional,
      "imageId"            -> strGen(12, 21).optional,
      "privateIp"          -> genIp.optional,
      "region"             -> strGen(1, 20).optional,
      "kernelId"           -> strGen(12, 20).optionalOrNull
    )
}

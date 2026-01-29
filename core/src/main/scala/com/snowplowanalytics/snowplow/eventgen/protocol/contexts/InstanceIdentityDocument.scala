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
import com.snowplowanalytics.snowplow.eventgen.primitives.IpAddress
import org.scalacheck.Gen
import io.circe.Json
import java.time.Instant

object InstanceIdentityDocument extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.amazon.aws.ec2", "instance_identity_document", "jsonschema", SchemaVer.Full(1, 0, 0))

  private val hexChar: Gen[Char] = Gen.oneOf(('0' to '9') ++ ('a' to 'f'))

  private val instanceIdGen: Gen[String] =
    Gen.stringOfN(17, hexChar).map(s => s"i-$s")

  private val imageIdGen: Gen[String] =
    Gen.stringOfN(17, hexChar).map(s => s"ami-$s")

  private val kernelIdGen: Gen[String] =
    Gen.stringOfN(8, hexChar).map(s => s"aki-$s")

  private val ramdiskIdGen: Gen[String] =
    Gen.stringOfN(8, hexChar).map(s => s"ari-$s")

  private val accountIdGen: Gen[String] =
    Gen.stringOfN(12, Gen.numChar)

  private val availabilityZoneGen: Gen[String] =
    Gen.oneOf("us-east-1a", "us-east-1b", "us-west-2a", "eu-west-1a", "ap-southeast-1a")

  private val regionGen: Gen[String] =
    Gen.oneOf("us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1", "ap-northeast-1")

  private val instanceTypeGen: Gen[String] =
    Gen.oneOf("t2.micro", "t2.small", "t2.medium", "t3.micro", "m5.large", "c5.xlarge")

  private val architectureGen: Gen[String] =
    Gen.oneOf("x86_64", "arm64", "i386")

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "instanceId"         -> instanceIdGen.required,
      "devpayProductCodes" -> Gen.listOfN(5, strGen(1, 5)).optionalOrNull,
      "billingProducts"    -> Gen.listOfN(5, strGen(1, 5)).optionalOrNull,
      "availabilityZone"   -> availabilityZoneGen.optional,
      "accountId"          -> accountIdGen.optional,
      "ramdiskId"          -> ramdiskIdGen.optionalOrNull,
      "architecture"       -> architectureGen.optional,
      "instanceType"       -> instanceTypeGen.optional,
      "version"            -> strGen(1, 20).optional,
      "pendingTime"        -> genInstant(now).map(_.toString).optional,
      "imageId"            -> imageIdGen.optional,
      "privateIp"          -> IpAddress.IpAddressV4.gen.map(_.repr).optional,
      "region"             -> regionGen.optional,
      "kernelId"           -> kernelIdGen.optionalOrNull
    )
}

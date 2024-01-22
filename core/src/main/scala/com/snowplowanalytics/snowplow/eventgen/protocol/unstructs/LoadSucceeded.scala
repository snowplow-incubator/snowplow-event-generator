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
package com.snowplowanalytics.snowplow.eventgen.protocol.unstructs

import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer}
import com.snowplowanalytics.snowplow.eventgen.protocol.SelfDescribingJsonGen
import com.snowplowanalytics.snowplow.eventgen.protocol.implicits._
import com.snowplowanalytics.snowplow.eventgen.primitives._
import org.scalacheck.Gen
import io.circe.{Json, JsonObject}
import io.circe.syntax.EncoderOps
import java.time.Instant

object LoadSucceeded extends SelfDescribingJsonGen {

  override def schemaKey: SchemaKey =
    SchemaKey("com.snowplowanalytics.monitoring.batch", "load_succeeded", "jsonschema", SchemaVer.Full(3, 0, 1))

  override def fieldGens(now: Instant): Map[String, Gen[Option[Json]]] =
    Map(
      "shredding"          -> shreddingGen(now).required,
      "application"        -> strGen(1, 128).required,
      "attempt"            -> Gen.chooseNum(1, 100).required,
      "loadingStarted"     -> genInstant(now).map(_.toString).required,
      "loadingCompleted"   -> genInstant(now).map(_.toString).required,
      "recoveryTableNames" -> Gen.listOfN(5, strGen(1, 5)).optional,
      "tags"               -> tagsGen.required
    )

  private def tagsGen: Gen[Json] = {
    val tupleGen = for {
      k <- strGen(1, 20)
      v <- strGen(1, 20)
    } yield k -> v.asJson
    Gen.listOfN(5, tupleGen).map { fields =>
      JsonObject.fromIterable(fields).asJson
    }
  }

  private def shreddingGen(now: Instant): Gen[Json] =
    Map(
      "base"        -> Url.gen.map(_.toString).required,
      "compression" -> Gen.oneOf("GZIP", "NONE").required,
      "typesInfo"   -> typesInfoGen.required,
      "timestamps"  -> timestampsGen(now).required,
      "processor"   -> processorGen.required
    ).genObject

  private def processorGen: Gen[Json] =
    Map(
      "artifact" -> strGen(1, 64).required,
      "version"  -> strGen(1, 16).required
    ).genObject

  private def timestampsGen(now: Instant): Gen[Json] =
    Map(
      "jobStarted"   -> genInstant(now).required,
      "jobCompleted" -> genInstant(now).required,
      "min"          -> genInstant(now).orNull,
      "max"          -> genInstant(now).orNull
    ).genObject

  private def typesInfoGen: Gen[Json] =
    Map(
      "transformation" -> Gen.const("WIDEROW").required,
      "fileFormat"     -> Gen.oneOf("JSON", "PARQUET").required,
      "types"          -> Gen.listOfN(5, typeGen).required
    ).genObject

  private def typeGen: Gen[Json] =
    Map(
      "schemaKey"      -> strGen(1, 256).required,
      "snowplowEntity" -> Gen.oneOf("SELF_DESCRIBING_EVENT", "CONTEXT").required
    ).genObject

}

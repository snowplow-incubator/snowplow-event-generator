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
package com.snowplowanalytics.snowplow.eventgen

import scala.concurrent.duration.DurationInt

import cats.effect.{IO, Resource}
import cats.effect.testing.specs2.CatsResource
import cats.syntax.all._

import com.snowplowanalytics.iglu.client.resolver.registries.JavaNetRegistryLookup._
import com.snowplowanalytics.iglu.client.resolver.registries.Registry
import com.snowplowanalytics.iglu.client.resolver.Resolver
import com.snowplowanalytics.iglu.client.IgluCirceClient

import com.snowplowanalytics.snowplow.eventgen.protocol.SelfDescribingJsonGen
import com.snowplowanalytics.snowplow.eventgen.protocol.contexts.AllContexts
import com.snowplowanalytics.snowplow.eventgen.protocol.unstructs.AllUnstructs

import org.scalacheck.Gen
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.core.Fragments

import java.net.URI

/** Validates that all context and unstruct event generators produce JSON that passes schema validation against Iglu
  * Central.
  */
class SchemaValidationSpec extends CatsResource[IO, IgluCirceClient[IO]] with SpecificationLike {
  import SchemaValidationSpec._

  skipAllIf(SchemaValidationSpec.javaVersion < 11)

  override val Timeout = 2.minutes

  override val resource: Resource[IO, IgluCirceClient[IO]] =
    for {
      resolver <- Resource.eval(Resolver.init[IO](cacheSize = 500, cacheTtl = None, refs = IgluCentral))
      client   <- Resource.eval(IgluCirceClient.fromResolver[IO](resolver, cacheSize = 500))
    } yield client

  "All context generators" should {
    Fragments.foreach(AllContexts.all) { gen =>
      s"generate valid JSON for ${gen.schemaKey.toSchemaUri}" >> {
        testGenerator(gen)
      }
    }
  }

  "All unstruct event generators" should {
    Fragments.foreach(AllUnstructs.all) { gen =>
      s"generate valid JSON for ${gen.schemaKey.toSchemaUri}" >> {
        testGenerator(gen)
      }
    }
  }

  private def testGenerator(gen: SelfDescribingJsonGen) = withResource { client =>
    for {
      now <- IO.realTimeInstant
      samples <- IO.fromOption(Gen.listOfN(SamplesPerSchema, gen.gen(now)).sample)(
        new RuntimeException(s"Failed to generate samples for ${gen.schemaKey.toSchemaUri}")
      )
      results <- samples.parTraverse { sdj =>
        client.check(sdj).value.map {
          case Right(_)  => None
          case Left(err) => Some(s"${sdj.schema.toSchemaUri}: ${err.toString}")
        }
      }
      failures = results.flatten
    } yield failures must beEmpty
  }
}

object SchemaValidationSpec {

  val SamplesPerSchema = 1000

  val IgluCentral: Registry.Http = Registry.Http(
    Registry.Config("Iglu Central", 0, List.empty),
    Registry.HttpConnection(URI.create("http://iglucentral.com"), None)
  )

  val javaVersion: Int = {
    val version = System.getProperty("java.version")
    """^(1\.)?(\d+)""".r.findFirstMatchIn(version).map(_.group(2).toInt).getOrElse(0)
  }
}

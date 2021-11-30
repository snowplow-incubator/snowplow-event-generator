/**
 * Copyright (c) 2014-2021 Snowplow Analytics Ltd. All rights reserved.
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

lazy val commonSettings = BuildSettings.commonSettings ++
  BuildSettings.sbtSiteSettings ++
  BuildSettings.basicSettigns ++
  BuildSettings.publishSettings ++
  BuildSettings.scoverage ++
  BuildSettings.dockerSettings ++
  BuildSettings.dynVerSettings

lazy val core = project
  .settings(
    moduleName := "snowplow-event-generator-core",
    description := "Generate random enriched events"
  )
  .enablePlugins(SiteScaladocPlugin, DockerPlugin, JavaAppPackaging)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    Dependencies.Libraries.collectionCompat,
    Dependencies.Libraries.analyticsSdk,
    Dependencies.Libraries.scalaCheck,
    Dependencies.Libraries.scalaCheckCats,
    Dependencies.Libraries.badRows,
    Dependencies.Libraries.httpClient,
    Dependencies.Libraries.snowplowRawEvent,
    Dependencies.Libraries.collectorPayload,
    Dependencies.Libraries.slf4j,
    // Scala (test only)
    Dependencies.Libraries.specs2Scalacheck,
    Dependencies.Libraries.specs2,
    Dependencies.Libraries.specs2Cats
  ))

lazy val fileSink = project
  .settings(
    moduleName := "snowplow-event-generator-file-sink"
  )
  .enablePlugins(SiteScaladocPlugin, DockerPlugin, JavaAppPackaging)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    Dependencies.Libraries.decline,
    Dependencies.Libraries.pureconfig,
    Dependencies.Libraries.pureconfigCirce,
    Dependencies.Libraries.fs2,
    Dependencies.Libraries.fs2file,
  ))
  .dependsOn(core)

lazy val root = project.aggregate(core, fileSink)
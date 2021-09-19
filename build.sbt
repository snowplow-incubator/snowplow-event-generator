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

lazy val core = project.in(file("."))
  .settings(
    name               := "snowplow-event-generator",
    description        := "Generate random enriched events",
  )
  .enablePlugins(SiteScaladocPlugin)
  .settings(BuildSettings.commonSettings)
  .settings(BuildSettings.sbtSiteSettings)
  .settings(BuildSettings.basicSettigns)
  .settings(BuildSettings.publishSettings)
  .settings(BuildSettings.scoverage)
  .settings(libraryDependencies ++= Seq(
    Dependencies.Libraries.fs2,
    Dependencies.Libraries.blobstore,
    Dependencies.Libraries.analyticsSdk,
    Dependencies.Libraries.scalaCheck,
    // Scala (test only)
    Dependencies.Libraries.specs2,
    Dependencies.Libraries.specs2Scalacheck,
    Dependencies.Libraries.specs2Cats
  ))


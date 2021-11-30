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

import sbt._
import Keys._

import sbtdynver.DynVerPlugin.autoImport._

import scoverage.ScoverageKeys._

import com.typesafe.sbt.site.SitePlugin.autoImport._
import com.typesafe.sbt.site.SiteScaladocPlugin.autoImport._
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.site.preprocess.PreprocessPlugin.autoImport._

object BuildSettings {

  lazy val commonSettings = Seq(
    name := "snowplow-event-generator",
    description := "Generate random events",
    organization := "com.snowplowanalytics",
    scalaVersion := "2.12.14",
    crossScalaVersions := Seq( "2.12.14", "2.13.6"),
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))
  )

  lazy val basicSettigns = Seq(
    resolvers ++= Dependencies.resolutionRepos
  )

  lazy val dynVerSettings = Seq(
    ThisBuild / dynverVTagPrefix := false, // Otherwise git tags required to have v-prefix
    ThisBuild / dynverSeparator := "-" // to be compatible with docker
  )

  /** Docker image settings */
  lazy val dockerSettings = Seq(
    Docker / maintainer := "Snowplow Analytics Ltd. <support@snowplowanalytics.com>",
    dockerBaseImage := "adoptopenjdk:11-jre-hotspot-focal",
    Docker / daemonUser := "daemon",
    dockerUpdateLatest := true,
    dockerRepository := Some("snowplow"),
    Docker / daemonUserUid := None,
    Docker / defaultLinuxInstallLocation := "/opt/snowplow",
  )

  // Maven Central publishing settings
  lazy val publishSettings = Seq[Setting[_]](
    pomIncludeRepository := { _ => false },
    ThisBuild / dynverVTagPrefix := false, // Otherwise git tags required to have v-prefix
    homepage := Some(url("http://snowplowanalytics.com")),
    scmInfo := Some(ScmInfo(url("https://github.com/snowplow-incubator/snowplow-event-generator"), "scm:git@github.com:snowplow-incubator/snowplow-event-generator.git")),
    publishArtifact := true,
    Test / publishArtifact := false,
    developers := List(
      Developer(
        "Snowplow Analytics Ltd",
        "Snowplow Analytics Ltd",
        "support@snowplowanalytics.com",
        url("https://snowplowanalytics.com")
      )
    )
  )

  val scoverage = Seq(
    coverageMinimumStmtTotal := 50,
    coverageFailOnMinimum := true,
    coverageHighlighting := false,
    (Test / test) := {
      (coverageReport dependsOn (Test / test)).value
    }
  )

  lazy val sbtSiteSettings = Seq(
    (SiteScaladoc / siteSubdirName) := s"${version.value}"
  )
}

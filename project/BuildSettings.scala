/** Copyright (c) 2014-2022 Snowplow Analytics Ltd. All rights reserved.
  *
  * This program is licensed to you under the Apache License Version 2.0, and you may not use this file except in
  * compliance with the Apache License Version 2.0. You may obtain a copy of the Apache License Version 2.0 at
  * http://www.apache.org/licenses/LICENSE-2.0.
  *
  * Unless required by applicable law or agreed to in writing, software distributed under the Apache License Version 2.0
  * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * Apache License Version 2.0 for the specific language governing permissions and limitations there under.
  */

import sbt._
import Keys._

import sbtdynver.DynVerPlugin.autoImport._

import scoverage.ScoverageKeys._

import sbtassembly._
import sbtassembly.AssemblyKeys._
import sbtassembly.AssemblyPlugin.defaultUniversalScript

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
    scalaVersion := "2.13.6",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))
  )

  lazy val basicSettigns = Seq(
    resolvers ++= Dependencies.resolutionRepos
  )

  lazy val dynVerSettings = Seq(
    ThisBuild / dynverVTagPrefix := false, // Otherwise git tags required to have v-prefix
    ThisBuild / dynverSeparator := "-"     // to be compatible with docker
  )

  /** Docker image settings */
  lazy val dockerSettings = Seq(
    Docker / maintainer := "Snowplow Analytics Ltd. <support@snowplowanalytics.com>",
    dockerBaseImage := "adoptopenjdk:11-jre-hotspot-focal",
    Docker / daemonUser := "daemon",
    dockerUpdateLatest := true,
    dockerRepository := Some("snowplow"),
    Docker / daemonUserUid := None,
    Docker / defaultLinuxInstallLocation := "/opt/snowplow"
  )

  // Maven Central publishing settings
  lazy val publishSettings = Seq[Setting[_]](
    pomIncludeRepository := { _ => false },
    ThisBuild / dynverVTagPrefix := false, // Otherwise git tags required to have v-prefix
    homepage := Some(url("http://snowplowanalytics.com")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/snowplow-incubator/snowplow-event-generator"),
        "scm:git@github.com:snowplow-incubator/snowplow-event-generator.git"
      )
    ),
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

  lazy val assemblySettings = Seq(
    assembly / assemblyMergeStrategy := {
      case x if x.endsWith("module-info.class")                   => MergeStrategy.discard
      case PathList("org", "apache", "commons", "logging", _ @_*) => MergeStrategy.first
      case PathList("META-INF", "MANIFEST.MF")                    => MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties")   => MergeStrategy.discard
      // case PathList("META-INF", _ @ _*) => MergeStrategy.discard    // Replaced with above for Stream Shredder
      case PathList("reference.conf", _ @_*)    => MergeStrategy.concat
      case PathList("codegen-resources", _ @_*) => MergeStrategy.first // Part of AWS SDK v2
      case "mime.types"                         => MergeStrategy.first // Part of AWS SDK v2
      case "AUTHORS"                            => MergeStrategy.discard
      case PathList("org", "slf4j", "impl", _)  => MergeStrategy.first
      case PathList("buildinfo", _)             => MergeStrategy.first
      case x if x.contains("javax")             => MergeStrategy.first
      case PathList("scala", "annotation", "nowarn.class" | "nowarn$.class") => MergeStrategy.first // http4s, 2.13 shim
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    assembly / assemblyJarName := s"${moduleName.value}-${version.value}.jar"
  ) ++ (if (sys.env.get("SKIP_TEST").contains("true")) Seq(assembly / test := {}) else Seq())

  lazy val executableSettings = Seq(
    // Executable jarfile
    assembly / assemblyPrependShellScript := Some(defaultUniversalScript(shebang = true)),

    // Name it as an executable
    assembly / assemblyJarName := name.value
  )

  val scoverage = Seq(
    coverageMinimumStmtTotal := 50,
    coverageFailOnMinimum := true,
    coverageHighlighting := false,
    (Test / test) := {
      coverageReport.dependsOn(Test / test).value
    }
  )

  lazy val sbtSiteSettings = Seq(
    (SiteScaladoc / siteSubdirName) := s"${version.value}"
  )
}

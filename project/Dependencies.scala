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

object Dependencies {

  val resolutionRepos = Seq(
    "Snowplow Analytics Maven repo".at("http://maven.snplow.com/releases/").withAllowInsecureProtocol(true)
  )

  libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.32" % Test

  object V {
    // Scala
    val analyticsSdk   = "2.1.0"
    val fs2            = "3.11.0"
    val decline        = "2.5.0"
    val blobstore      = "0.9.5"
    val scalaCheckCats = "0.3.1"
    val catsRetry      = "3.1.3"
    val kcl            = "2.4.0"
    val slf4j          = "1.7.32"
    val circeConfig    = "0.10.1"
    val circe          = "0.14.10"
    val circeExtras    = "0.14.4"
    val fs2Pubsub      = "0.22.0"
    val fs2Kafka       = "3.9.1"
    val awsSdk         = "2.35.9"
    // Scala (test only)
    val specs2           = "4.21.0"
    val scalaCheck       = "1.18.1"
    val collectionCompat = "2.14.0"
    val igluClient       = "3.1.0"
    val catsEffect       = "3.7.0"
    val catsEffectSpecs2 = "1.6.0"
    // raw output
    val snowplowRawEvent = "0.1.0"
    val collectorPayload = "0.0.0"
    val badRows          = "2.1.1"
    val httpClient       = "4.5.14"
    val thrift           = "0.22.0"
    val http4s           = "0.23.33"
  }

  object Libraries {
    // Scala
    val analyticsSdk   = "com.snowplowanalytics"    %% "snowplow-scala-analytics-sdk" % V.analyticsSdk
    val fs2            = "co.fs2"                   %% "fs2-core"                     % V.fs2
    val fs2file        = "co.fs2"                   %% "fs2-io"                       % V.fs2
    val fs2Pubsub      = "com.permutive"            %% "fs2-google-pubsub-grpc"       % V.fs2Pubsub
    val fs2Kafka       = "com.github.fd4s"          %% "fs2-kafka"                    % V.fs2Kafka
    val decline        = "com.monovore"             %% "decline"                      % V.decline
    val blobstore      = "com.github.fs2-blobstore" %% "s3"                           % V.blobstore
    val circeCore      = "io.circe"                 %% "circe-core"                   % V.circe
    val circeConfig    = "io.circe"                 %% "circe-config"                 % V.circeConfig
    val circeGeneric   = "io.circe"                 %% "circe-generic"                % V.circe
    val circeParser    = "io.circe"                 %% "circe-parser"                 % V.circe
    val circeExtras    = "io.circe"                 %% "circe-generic-extras"         % V.circeExtras
    val scalaCheck     = "org.scalacheck"           %% "scalacheck"                   % V.scalaCheck
    val scalaCheckCats = "io.chrisdavenport"        %% "cats-scalacheck"              % V.scalaCheckCats
    val catsRetry      = "com.github.cb372"         %% "cats-retry"                   % V.catsRetry
    val httpClient     = "org.apache.httpcomponents" % "httpclient"                   % V.httpClient
    val slf4j          = "org.slf4j"                 % "slf4j-simple"                 % V.slf4j
    val kcl            = "software.amazon.awssdk"    % "kinesis"                      % V.awsSdk
    val awsRegions     = "software.amazon.awssdk"    % "regions"                      % V.awsSdk
    val http4sClient   = "org.http4s"               %% "http4s-blaze-client"          % V.http4s
    val stsSdk         = "software.amazon.awssdk"    % "sts"                          % V.awsSdk
    val http4sEmber    = "org.http4s"               %% "http4s-ember-client"          % V.http4s
    val http4sCirce    = "org.http4s"               %% "http4s-circe"                 % V.http4s

    // Scala (test only)
    val specs2           = "org.specs2"            %% "specs2-core"                % V.specs2           % Test
    val specs2Cats       = "org.specs2"            %% "specs2-cats"                % V.specs2           % Test
    val specs2Scalacheck = "org.specs2"            %% "specs2-scalacheck"          % V.specs2           % Test
    val igluClient       = "com.snowplowanalytics" %% "iglu-scala-client"          % V.igluClient       % Test
    val catsEffect       = "org.typelevel"         %% "cats-effect"                % V.catsEffect       % Test
    val catsEffectSpecs2 = "org.typelevel"         %% "cats-effect-testing-specs2" % V.catsEffectSpecs2 % Test

    val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % V.collectionCompat
    val thrift           = "org.apache.thrift"       % "libthrift"               % V.thrift
    // raw output
    val snowplowRawEvent = "com.snowplowanalytics"  % "snowplow-thrift-raw-event" % V.snowplowRawEvent
    val collectorPayload = "com.snowplowanalytics"  % "collector-payload-1"       % V.collectorPayload
    val badRows          = "com.snowplowanalytics" %% "snowplow-badrows"          % V.badRows
  }
}

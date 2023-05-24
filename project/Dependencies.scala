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
    val fs2            = "3.1.1"
    val decline        = "2.1.0"
    val blobstore      = "0.9.5"
    val scalaCheckCats = "0.3.1"
    val kcl            = "2.4.0"
    val slf4j          = "1.7.32"
    val circeConfig    = "0.8.0"
    val circe          = "0.14.1"
    val fs2Pubsub      = "0.21.0"
    val awsRegions     = "2.20.69"
    // Scala (test only)
    val specs2           = "4.12.3"
    val scalaCheck       = "1.14.0"
    val collectionCompat = "2.4.4"
    // raw output
    val snowplowRawEvent = "0.1.0"
    val collectorPayload = "0.0.0"
    val badRows          = "2.1.1"
    val httpClient       = "4.5.13"
    val thrift           = "0.15.0" // override transitive dependency to mitigate security vulnerabilities
  }

  object Libraries {
    // Scala
    val analyticsSdk   = "com.snowplowanalytics"    %% "snowplow-scala-analytics-sdk" % V.analyticsSdk
    val fs2            = "co.fs2"                   %% "fs2-core"                     % V.fs2
    val fs2file        = "co.fs2"                   %% "fs2-io"                       % V.fs2
    val fs2Pubsub      = "com.permutive"            %% "fs2-google-pubsub-grpc"       % V.fs2Pubsub
    val decline        = "com.monovore"             %% "decline"                      % V.decline
    val blobstore      = "com.github.fs2-blobstore" %% "s3"                           % V.blobstore
    val circeCore      = "io.circe"                 %% "circe-core"                   % V.circe
    val circeConfig    = "io.circe"                 %% "circe-config"                 % V.circeConfig
    val circeGeneric   = "io.circe"                 %% "circe-generic"                % V.circe
    val circeParser    = "io.circe"                 %% "circe-parser"                 % V.circe
    val circeExtras    = "io.circe"                 %% "circe-generic-extras"         % V.circe
    val scalaCheck     = "org.scalacheck"           %% "scalacheck"                   % V.scalaCheck
    val scalaCheckCats = "io.chrisdavenport"        %% "cats-scalacheck"              % V.scalaCheckCats
    val httpClient     = "org.apache.httpcomponents" % "httpclient"                   % V.httpClient
    val slf4j          = "org.slf4j"                 % "slf4j-simple"                 % V.slf4j
    val kcl            = "software.amazon.kinesis"   % "amazon-kinesis-client"        % V.kcl
    val awsRegions     = "software.amazon.awssdk"    % "regions"                      % V.awsRegions

    // Scala (test only)
    val specs2           = "org.specs2" %% "specs2-core"       % V.specs2 % Test
    val specs2Cats       = "org.specs2" %% "specs2-cats"       % V.specs2 % Test
    val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % V.specs2 % Test

    val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % V.collectionCompat
    val thrift           = "org.apache.thrift"       % "libthrift"               % V.thrift
    // raw output
    val snowplowRawEvent = "com.snowplowanalytics"  % "snowplow-thrift-raw-event" % V.snowplowRawEvent
    val collectorPayload = "com.snowplowanalytics"  % "collector-payload-1"       % V.collectorPayload
    val badRows          = "com.snowplowanalytics"  %% "snowplow-badrows"          % V.badRows
  }
}

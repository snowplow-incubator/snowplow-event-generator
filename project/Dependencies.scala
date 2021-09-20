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

object Dependencies {
  object V {
    // Scala
    val analyticsSdk     = "2.1.0"
    val fs2              = "2.5.9"
    val decline          = "2.1.0"
    val blobstore        = "0.8.4"
    val pureconfig       = "0.16.0"

    // Scala (test only)
    val specs2           = "4.12.3"
    val scalaCheck       = "1.14.0"
  }

  object Libraries {
    // Scala
    val analyticsSdk     = "com.snowplowanalytics"      %% "snowplow-scala-analytics-sdk" % V.analyticsSdk
    val fs2              = "co.fs2"                     %% "fs2-core"                     % V.fs2
    val decline          = "com.monovore"               %% "decline"                      % V.decline
    val blobstore        = "com.github.fs2-blobstore"   %% "s3"                           % V.blobstore
    val pureconfig       = "com.github.pureconfig"      %% "pureconfig"                   % V.pureconfig
    val pureconfigCirce  = "com.github.pureconfig"      %% "pureconfig-circe"             % V.pureconfig

    val scalaCheck       = "org.scalacheck"             %% "scalacheck"                   % V.scalaCheck

    // Scala (test only)
    val specs2           = "org.specs2"                 %% "specs2-core"                  % V.specs2     % Test
    val specs2Cats       = "org.specs2"                 %% "specs2-cats"                  % V.specs2     % Test
    val specs2Scalacheck = "org.specs2"                 %% "specs2-scalacheck"            % V.specs2     % Test
  }
}

/*
 * Copyright (c) 2021 Snowplow Analytics Ltd. All rights reserved.
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

import cats.effect.{IO, IOApp, ExitCode}


object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    args match {
      case path :: Nil =>
        Config.fromPath[IO](path).flatMap {
          case Right(config) => Sink.run[IO]("s3://production-data-samples-frankfurt/random-5/", config).as(ExitCode.Success)
          case Left(error) => IO(System.err.println(error)).as(ExitCode.Error)
        }
      case _ =>
        IO(System.err.println(s"The app requires single path arg. ${args.length} provided")).as(ExitCode.Error)
    }
}

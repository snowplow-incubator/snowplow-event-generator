/*
 * Copyright (c) 2021-2023 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest

import fs2.{Pipe, Stream}

import cats.syntax.all._
import cats.effect.kernel.Sync

import scalaj.http.{Http => HttpClient}

import cats.effect.Async

object Http {

    def sink[F[_]: Async](properties: Config.Output.Http): Pipe[F, Main.GenOutput, Unit] = {
         
         def mkTp2(
            generatedRequest: HttpRequest
        ): (String, String) = {
            // val endpoint = properties.endpoint
            val uri = "http://%s/com.snowplowanalytics.snowplow/tp2".format(properties.endpoint)
            val body = generatedRequest.body match {
                case Some(b) => b.toString()
                case _ => ""
            }

            (uri, body)
        }

        st: Stream[F, Main.GenOutput] => 
          st.map(_._3)
            .map(mkTp2)
            .evalMap(e => 
              Sync[F].delay(
                e._2 match {
                  case "" => // Do nothing if the body is empty
                  case _  => HttpClient(e._1)
                    .postData(e._2)
                    .header("Content-Type", "application/json")
                    .asString
                }
                
              )
            ).map(println)
            .void
    }
}

// TODO:
  // Use a client that's not deprecated (http4s?)
  // Figure out how to parse the headers that are provided in the gen method
  // Do soemthing with qs requests
  // Maybe make it configurable how much of each we produce?

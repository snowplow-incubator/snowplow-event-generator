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


// TODO: This is wrecking my head.
// This client might be simpler: https://stackoverflow.com/questions/11719373/doing-http-request-in-scala


object Http {

    def sink[F[_]: Async](properties: Config.Output.Http): Pipe[F, Main.GenOutput, Unit] = {
         def mkTp2(
            generatedRequest: HttpRequest
        ): (String, String) = {
            val endpoint = properties.endpoint
            val uri = "http://%s/com.snowplowanalytics.snowplow/tp2".format(endpoint)
            val body = generatedRequest.body match {
                case Some(b) => b.toString()
                case _ => ""
            }
            (uri, body)
        }

        /*
        def write(properties: Config.Output.Http, data: HttpRequest): Pipe[F, HttpRequest, Unit] = {
            client.run()
            //use(c => c.run(mkTp2(properties.endpoint, data)))
            }
        */

        //   type GenOutput = (collector.CollectorPayload, List[Event], HttpRequest)

        st: Stream[F, Main.GenOutput] =>
          st.map(_._3)
            .map(mkTp2)
            .parEvalMap(10)(e =>
              Sync[F].delay(
                HttpClient(e._1).postData(e._2)
              )
            ).void
            //.map(client.run(_))
            // .map(client.use(c => c.run(_)))
    }
    /*
   val result = Http("http://example.com/url").postData("""{"id":"12","json":"data"}""")
    .header("Content-Type", "application/json")
    .header("Charset", "UTF-8")
    .option(HttpOptions.readTimeout(10000)).asString
*/
}


/*

def reqBuilder(event: Event): PutRecordRequest = PutRecordRequest
        .builder()
        .streamName(output.streamName)
        .partitionKey(event.event_id.toString)
        .data(SdkBytes.fromUtf8String(event.toTsv))
        .build()

      st: Stream[F, GenOutput] =>
        st.map(_._2)
          .flatMap(Stream.emits)
          .map(reqBuilder)
          .parEvalMap(10)(e => Async[F].fromCompletableFuture(Sync[F].delay(kinesisClient.putRecord(e))))
          .void

          */

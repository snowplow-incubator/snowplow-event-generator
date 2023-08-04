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
import cats.effect.{ContextShift, IO, Async}
import cats.effect.kernel.Sync

// import org.http4s.{Request, Response, Status}
import org.http4s.{Request, Method, Uri}
// import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

object Http {

    private val executionContext = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(executionContext)
    //def mkClient: Resource[IO, Client[IO]] =
    val client = BlazeClientBuilder[IO](executionContext).resource

   

    def sink[F[_]: Async](properties: Config.Output.Http): Pipe[F, Main.GenOutput, Unit] = {
         def mkTp2(
            generatedRequest: HttpRequest
        ) = {
            val endpoint = properties.endpoint
            val uri = Uri.unsafeFromString(s"http://$endpoint/com.snowplowanalytics.snowplow/tp2")
            val body = generatedRequest.body match {
                case Some(b) => b.toString()
                case _ => ""
            }
            Request[IO](Method.POST, uri).withEntity(body)
        }

        /*
        def write(properties: Config.Output.Http, data: HttpRequest): Pipe[F, HttpRequest, Unit] = {
            client.run()
            //use(c => c.run(mkTp2(properties.endpoint, data)))
            }
        */
        
        st: Stream[F, Main.GenOutput] => 
            st.map(_._3)
            .flatMap(Stream.emits)
            .map(mkTp2(_))
            .parEvalMap(10)(e => Async[F].fromCompletableFuture(Sync[F].delay(client.run(e))))
            //.map(client.run(_))
            // .map(client.use(c => c.run(_)))
            .void

    }

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

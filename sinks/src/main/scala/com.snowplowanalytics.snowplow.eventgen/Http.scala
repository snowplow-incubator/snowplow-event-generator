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
import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest.{Method => TrackerMethod}

import fs2.{Pipe, Stream}

import cats.syntax.all._

import cats.effect.Async

import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Request
import org.http4s.Header.Raw
import org.http4s.Method
import org.http4s.Uri

import org.typelevel.ci._
import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequestQuerystring


object Http {

  def sink[F[_]: Async](properties: Config.Output.Http): Pipe[F, Main.GenOutput, Unit] = {

    val baseUri = Uri.fromString(properties.endpoint) match {
        case Right(value) => value
        case Left(_) =>
          throw new IllegalArgumentException(
            s"Error: cannot produce uri from endpoint provided: ${properties.endpoint}"
          )
      }


    def buildRequesst(
      generatedRequest: HttpRequest
    ): Request[F] = {

      // Origin headers are given to us a key and a comma separated string of values, 
      // but we wish to attach each value as a header with the provided key ("Origin")
      // This function provides a Seq of Raw headers (the type that putHeaders expects) 
      // containing an item for each origin header, and an item each for the other headers.
      def parseHeaders(headers: Map[String, String]): Seq[Raw] = {
        headers
          .map(kv => (kv._1, kv._2) match {
            case (k, v) if k == "Origin" =>  
              v.split(",")
                .map(v => Raw(CIString(k), v))
                .toSeq
              
              // Raw(CIString(k), v)
            case (_, _) => Seq(Raw(CIString(kv._1), kv._2))
          })
          .toSeq
          .flatten
      }

       // since /i requests have empty version, we pattern match to add the path.
       // address is a var since we modify it when adding querystring
       var address = generatedRequest.method.path.version match {
        case "" => baseUri / generatedRequest.method.path.vendor
        case version: String  => baseUri / generatedRequest.method.path.vendor / version
       }

      val body = generatedRequest.body match {
        case Some(b) => b.toString()
        case _       => ""
      }

      val req = generatedRequest.method match {

        // POST requests: attach body, use the uri we already have, without adding querysring
        case TrackerMethod.Post(_) =>
          Request[F](method = Method.POST, uri = address).withEntity(body).putHeaders(parseHeaders(generatedRequest.headers))

        // GET requests: add querystring, ignore body field
        case TrackerMethod.Get(_) =>
                    
          // iterate querystring and add as kv pairs to the address
          generatedRequest.qs match {
            case None => address
            case Some(querystring: HttpRequestQuerystring) => 
              // val newUri = address
              querystring.toProto
              querystring.toProto.foreach(kv =>
                address = address.withQueryParam(kv.getName(), kv.getValue())
              )
          }

          Request[F](method = Method.GET, uri = address).putHeaders(parseHeaders(generatedRequest.headers))

        // HEAD requests induce server errors that will take some digging to figure out.
        // In the interest of getting to a usable tool quickly, for now we just throw an error for these.
        case TrackerMethod.Head(_) => throw new NotImplementedError ( "HEAD requests not implemented" )

        /*
            generatedRequest.qs match {
            case None => address
            case Some(querystring: HttpRequestQuerystring) => 
              // val newUri = address
              querystring.toProto
              querystring.toProto.foreach(kv =>
                address = address.withQueryParam(kv.getName(), kv.getValue())
              )
          }
          

          Request[F](method = Method.HEAD, uri = address).putHeaders(parseHeaders(generatedRequest.headers))
          */

          // This hits org.http4s.client.UnexpectedStatus: unexpected HTTP status: 422 Unprocessable Entity for request HEAD 

      }
      // TODO: Some code repetition can probably be removed from this

      return req
    }

    val httpClient = EmberClientBuilder.default[F].build

    st: Stream[F, Main.GenOutput] => {
        Stream
          .resource(httpClient)
          .flatMap(client => 
            st.map(_._3).map(buildRequesst).evalMap(req => client.expect[String](req)).void)
    }
  }
}

/*
 * Copyright (c) 2021-2025 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.eventgen.sinks

import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest
import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequest.{Method => TrackerMethod}

import fs2.{Pipe, Stream}

import cats.syntax.all._

import cats.effect.kernel.Async

import org.http4s.ember.client.EmberClientBuilder
import org.http4s.Request
import org.http4s.Header.Raw
import org.http4s.Method

import org.typelevel.ci._

import com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload
import com.snowplowanalytics.snowplow.eventgen.tracker.HttpRequestQuerystring
import com.snowplowanalytics.snowplow.eventgen.Config

object Http {

  def make[F[_]: Async](config: Config.Output.Http) = new Sink[F] {

    override def collectorPayload: Pipe[F, CollectorPayload, Unit] =
      _ => Stream.raiseError(new IllegalStateException(s"Can't use HTTP output for Thrift collector payloads"))

    override def enriched: Pipe[F, String, Unit] =
      _ => Stream.raiseError(new IllegalStateException(s"Can't use HTTP output for enriched events"))

    override def http: Pipe[F, HttpRequest, Unit] =
      requests =>
        Stream
          .resource(EmberClientBuilder.default[F].build)
          .flatMap(client =>
            requests.map(buildRequest[F](config, _)).parEvalMap(Runtime.getRuntime.availableProcessors * 100) { req =>
              client
                .status(req)
                .void
                // The app also generates bad requests (e.g. to bad path).
                // We don't want the app to crash when the collector replies with some error.
                .voidError
            }
          )
  }

  private def buildRequest[F[_]](
    config: Config.Output.Http,
    generatedRequest: HttpRequest
  ): Request[F] = {

    // Origin headers are given to us a key and a comma separated string of values,
    // but we wish to attach each value as a header with the provided key ("Origin")
    // This function provides a Seq of Raw headers (the type that putHeaders expects)
    // containing an item for each origin header, and an item each for the other headers.
    def parseHeaders(headers: Map[String, String]): Seq[Raw] =
      headers
        .map(kv =>
          (kv._1, kv._2) match {
            case (k, v) if k == "Origin" =>
              v.split(",").map(v => Raw(CIString(k), v)).toSeq

            // Raw(CIString(k), v)
            case (_, _) => Seq(Raw(CIString(kv._1), kv._2))
          }
        )
        .toSeq
        .flatten

    // since /i requests have empty version, we pattern match to add the path.
    // address is a var since we modify it when adding querystring
    var address = generatedRequest.method.path.version match {
      case ""              => config.endpoint / generatedRequest.method.path.vendor
      case version: String => config.endpoint / generatedRequest.method.path.vendor / version
    }

    val body = generatedRequest.body match {
      case Some(b) => b.toString()
      case _       => ""
    }

    generatedRequest.method match {

      // POST requests: attach body, use the uri we already have, without adding querysring
      case TrackerMethod.Post(_) =>
        Request[F](method = Method.POST, uri = address)
          .withEntity(body)
          .putHeaders(parseHeaders(generatedRequest.headers))

      // GET requests: add querystring, ignore body field
      case TrackerMethod.Get(_) =>
        // iterate querystring and add as kv pairs to the address
        generatedRequest.qs match {
          case None                                      => address
          case Some(querystring: HttpRequestQuerystring) =>
            // val newUri = address
            querystring.toProto
            querystring.toProto.foreach(kv => address = address.withQueryParam(kv.getName(), kv.getValue()))
        }

        Request[F](method = Method.GET, uri = address).putHeaders(parseHeaders(generatedRequest.headers))

      // HEAD requests induce server errors that will take some digging to figure out.
      // In the interest of getting to a usable tool quickly, for now we just throw an error for these.
      case TrackerMethod.Head(_) => throw new NotImplementedError("HEAD requests not implemented")
    }
  }
}

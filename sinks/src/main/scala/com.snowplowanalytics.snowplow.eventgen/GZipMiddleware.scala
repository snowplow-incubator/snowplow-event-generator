/*
 * Copyright (c) 2021-2022 Snowplow Analytics Ltd. All rights reserved.
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

import cats.effect._
import fs2.compression.Compression
import fs2.compression.DeflateParams
import fs2.compression.ZLibParams
import org.http4s.Header
import org.http4s.Request
import org.http4s.headers.`Content-Encoding`
import org.http4s.headers.`Content-Length`

object GZipMiddleware {
  val DefaultBufferSize = 32 * 1024

  def apply[F[_]: Sync](
    retainUserEncoding: Boolean,
    bufferSize: Int = DefaultBufferSize,
    level: DeflateParams.Level = DeflateParams.Level.DEFAULT
  )(request: Request[F]): Request[F] = {
    val updateContentTypeEncoding =
      (retainUserEncoding, request.headers.get[`Content-Encoding`]) match {
        case (true, Some(`Content-Encoding`(cc))) =>
          Header.Raw(
            `Content-Encoding`.headerInstance.name,
            s"${cc.coding}, gzip"
          )

        case _ =>
          Header.Raw(
            `Content-Encoding`.headerInstance.name,
            "gzip"
          )
      }
    val compressPipe =
      Compression
        .forSync[F]
        .gzip(
          fileName = None,
          modificationTime = None,
          comment = None,
          DeflateParams(
            bufferSize = bufferSize,
            level = level,
            header = ZLibParams.Header.GZIP
          )
        )
    request
      .removeHeader[`Content-Length`]
      .putHeaders(updateContentTypeEncoding)
      .withBodyStream(request.body.through(compressPipe))
  }

}

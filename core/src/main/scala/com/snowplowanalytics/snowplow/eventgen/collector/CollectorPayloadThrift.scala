/*
 * Copyright (c) 2012-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.eventgen.collector

import org.apache.thrift.protocol.{TBinaryProtocol, TField, TList, TProtocol, TStruct, TType}
import org.apache.thrift.transport.TIOStreamTransport

import java.io.ByteArrayOutputStream

/** A collector payload as the `CollectorPayload` Thrift struct (collector-payload-1), which is what the collector
  * writes to the raw stream and enrich reads from it.
  *
  * The field ids and types in the companion must match that struct's IDL, because readers match fields by id and skip
  * any id they don't recognise. A wrong id doesn't fail: the field silently goes missing downstream. The IDL is in
  * snowplow/snowplow at `2-collectors/thrift-schemas/collector-payload-1/src/main/thrift/collector-payload.thrift`.
  */
final private[collector] case class CollectorPayloadThrift(
  schema: String,
  ipAddress: Option[String],
  timestamp: Long,
  encoding: String,
  collector: String,
  userAgent: Option[String],
  refererUri: Option[String],
  path: Option[String],
  querystring: Option[String],
  body: Option[String],
  headers: Option[List[String]],
  contentType: Option[String],
  hostname: Option[String],
  networkUserId: Option[String]
) {
  import CollectorPayloadThrift._

  /** Serialized with the binary protocol, the default of libthrift's `TSerializer` */
  def toBytes: Array[Byte] = {
    val out = new ByteArrayOutputStream()
    write(new TBinaryProtocol(new TIOStreamTransport(out)))
    out.toByteArray
  }

  private def write(p: TProtocol): Unit = {
    p.writeStructBegin(Struct)
    writeString(p, IpAddress, ipAddress)
    p.writeFieldBegin(Timestamp)
    p.writeI64(timestamp)
    p.writeFieldEnd()
    writeString(p, Encoding, Some(encoding))
    writeString(p, Collector, Some(collector))
    writeString(p, UserAgent, userAgent)
    writeString(p, RefererUri, refererUri)
    writeString(p, Path, path)
    writeString(p, Querystring, querystring)
    writeString(p, Body, body)
    headers.foreach { hs =>
      p.writeFieldBegin(Headers)
      p.writeListBegin(new TList(TType.STRING, hs.size))
      hs.foreach(p.writeString)
      p.writeListEnd()
      p.writeFieldEnd()
    }
    writeString(p, ContentType, contentType)
    writeString(p, Hostname, hostname)
    writeString(p, NetworkUserId, networkUserId)
    writeString(p, Schema, Some(schema))
    p.writeFieldStop()
    p.writeStructEnd()
  }
}

private[collector] object CollectorPayloadThrift {

  private val Struct        = new TStruct("CollectorPayload")
  private val IpAddress     = new TField("ipAddress", TType.STRING, 100.toShort)
  private val Timestamp     = new TField("timestamp", TType.I64, 200.toShort)
  private val Encoding      = new TField("encoding", TType.STRING, 210.toShort)
  private val Collector     = new TField("collector", TType.STRING, 220.toShort)
  private val UserAgent     = new TField("userAgent", TType.STRING, 300.toShort)
  private val RefererUri    = new TField("refererUri", TType.STRING, 310.toShort)
  private val Path          = new TField("path", TType.STRING, 320.toShort)
  private val Querystring   = new TField("querystring", TType.STRING, 330.toShort)
  private val Body          = new TField("body", TType.STRING, 340.toShort)
  private val Headers       = new TField("headers", TType.LIST, 350.toShort)
  private val ContentType   = new TField("contentType", TType.STRING, 360.toShort)
  private val Hostname      = new TField("hostname", TType.STRING, 400.toShort)
  private val NetworkUserId = new TField("networkUserId", TType.STRING, 410.toShort)
  private val Schema        = new TField("schema", TType.STRING, 31337.toShort)

  private def writeString(p: TProtocol, field: TField, value: Option[String]): Unit =
    value.foreach { v =>
      p.writeFieldBegin(field)
      p.writeString(v)
      p.writeFieldEnd()
    }
}

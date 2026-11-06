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

import com.snowplowanalytics.snowplow.eventgen.{GenConfig, runGen}
import org.apache.thrift.protocol.{TBinaryProtocol, TType}
import org.apache.thrift.transport.TMemoryInputTransport
import org.specs2.mutable.Specification

import java.time.Instant
import scala.annotation.tailrec
import scala.util.Random

class CollectorPayloadThriftSpec extends Specification {
  import CollectorPayloadThriftSpec._

  "CollectorPayloadThrift.toBytes" should {

    // Expected bytes were produced by the classes generated from collector-payload-1 by the Thrift compiler
    "produce the same bytes as the generated Thrift classes" in {
      (hex(full.toBytes) must_== fullHex)
        .and(hex(minimal.toBytes) must_== minimalHex)
        .and(hex(emptyHeaders.toBytes) must_== emptyHeadersHex)
    }

    "write every set field under its collector-payload-1 id, and no other" in {
      List(full, minimal, emptyHeaders).map(t => decode(t.toBytes) must_== expectedFields(t)).reduce(_ and _)
    }

    "write every field of generated collector payloads" in {
      val random = new Random(42L)
      val gen = CollectorPayload.gen(
        GenConfig.EventsPerPayload(1, 5),
        Instant.parse("2026-11-05T00:00:00Z"),
        GenConfig.EventsFrequencies(1, 1, 1, 1, 1, 1, 1, Map.empty),
        GenConfig.ContextsPerEvent(0, 3),
        GenConfig.IdentitySource.NoIdentity,
        None,
        List("app")
      )
      List
        .fill(200)(runGen(gen, random))
        .map(cp => decode(cp.toRaw) must_== expectedFields(cp.thriftFields))
        .reduce(_ and _)
    }
  }
}

object CollectorPayloadThriftSpec {

  private val schema = "iglu:com.snowplowanalytics.snowplow/CollectorPayload/thrift/1-0-0"

  private val full = CollectorPayloadThrift(
    schema = schema,
    ipAddress = Some("203.0.113.7"),
    timestamp = 1762387200123L,
    encoding = "UTF-8",
    collector = "ssc-3.0.0-kinesis",
    userAgent = Some("Mozilla/5.0 (ünïcödé 😀)"),
    refererUri = Some("https://example.com/日本語?q=1"),
    path = Some("/com.snowplowanalytics.snowplow/tp2"),
    querystring = Some("e=pv&url=https%3A%2F%2Fexample.com"),
    body = Some("""{"schema":"iglu:x","data":[{"e":"ue","tv":"ü-😀"}]}"""),
    headers = Some(List("Host: example.com", "X-Ünïcödé: 日本語")),
    contentType = Some("application/json; charset=UTF-8"),
    hostname = Some("collector.example.com"),
    networkUserId = Some("c6ef3124-b53a-4b13-a233-0088f79dcbcb")
  )

  private val minimal =
    CollectorPayloadThrift(schema, None, 0L, "UTF-8", "c", None, None, None, None, None, None, None, None, None)

  private val emptyHeaders = minimal.copy(headers = Some(Nil), timestamp = -1L)

  private val fullHex =
    "0b00640000000b3230332e302e3131332e370a00c80000019a5676887b0b00d2000000055554462d380b00dc000000117373632d332e302e302d6b696e657369730b012c0000001e4d6f7a696c6c612f352e302028c3bc6ec3af63c3b664c3a920f09f9880290b01360000002168747470733a2f2f6578616d706c652e636f6d2fe697a5e69cace8aa9e3f713d310b0140000000232f636f6d2e736e6f77706c6f77616e616c79746963732e736e6f77706c6f772f7470320b014a00000022653d70762675726c3d68747470732533412532462532466578616d706c652e636f6d0b0154000000367b22736368656d61223a2269676c753a78222c2264617461223a5b7b2265223a227565222c227476223a22c3bc2df09f9880227d5d7d0f015e0b0000000200000011486f73743a206578616d706c652e636f6d00000018582dc39c6ec3af63c3b664c3a93a20e697a5e69cace8aa9e0b01680000001f6170706c69636174696f6e2f6a736f6e3b20636861727365743d5554462d380b019000000015636f6c6c6563746f722e6578616d706c652e636f6d0b019a0000002463366566333132342d623533612d346231332d613233332d3030383866373964636263620b7a690000004169676c753a636f6d2e736e6f77706c6f77616e616c79746963732e736e6f77706c6f772f436f6c6c6563746f725061796c6f61642f7468726966742f312d302d3000"

  private val minimalHex =
    "0a00c800000000000000000b00d2000000055554462d380b00dc00000001630b7a690000004169676c753a636f6d2e736e6f77706c6f77616e616c79746963732e736e6f77706c6f772f436f6c6c6563746f725061796c6f61642f7468726966742f312d302d3000"

  private val emptyHeadersHex =
    "0a00c8ffffffffffffffff0b00d2000000055554462d380b00dc00000001630f015e0b000000000b7a690000004169676c753a636f6d2e736e6f77706c6f77616e616c79746963732e736e6f77706c6f772f436f6c6c6563746f725061796c6f61642f7468726966742f312d302d3000"

  private def hex(bytes: Array[Byte]): String = bytes.map("%02x".format(_)).mkString

  private def expectedFields(t: CollectorPayloadThrift): Map[Short, Any] =
    Map[Int, Option[Any]](
      100   -> t.ipAddress,
      200   -> Some(t.timestamp),
      210   -> Some(t.encoding),
      220   -> Some(t.collector),
      300   -> t.userAgent,
      310   -> t.refererUri,
      320   -> t.path,
      330   -> t.querystring,
      340   -> t.body,
      350   -> t.headers,
      360   -> t.contentType,
      400   -> t.hostname,
      410   -> t.networkUserId,
      31337 -> Some(t.schema)
    ).collect { case (id, Some(value)) => id.toShort -> value }

  /** Reads a struct of string, i64 and list-of-string fields, keyed by field id */
  private def decode(bytes: Array[Byte]): Map[Short, Any] = {
    val p = new TBinaryProtocol(new TMemoryInputTransport(bytes))
    p.readStructBegin()

    @tailrec
    def go(acc: Map[Short, Any]): Map[Short, Any] = {
      val field = p.readFieldBegin()
      if (field.`type` == TType.STOP) acc
      else {
        val value: Any = field.`type` match {
          case TType.STRING => p.readString()
          case TType.I64    => p.readI64()
          case TType.LIST =>
            val list = p.readListBegin()
            List.fill(list.size)(p.readString())
          case other => throw new IllegalStateException(s"Unexpected Thrift type $other for field ${field.id}")
        }
        p.readFieldEnd()
        go(acc + (field.id -> value))
      }
    }

    go(Map.empty)
  }
}

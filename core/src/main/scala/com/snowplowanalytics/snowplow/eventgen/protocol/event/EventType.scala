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
package com.snowplowanalytics.snowplow.eventgen.protocol.event

import org.scalacheck.Gen

sealed trait EventType {
  override def toString: String = this match {
    case EventType.Struct => "se"
    case EventType.Unstruct => "ue"
    case EventType.PageView => "pv"
    case EventType.PagePing => "pp"
    // Commented types below not implemented in generator
    //    case EventType.StructLegacy => "ev"
    //    case EventType.AdImpression => "ad"
    //    case EventType.Transaction => "tr"
    //    case EventType.TransactionItem => "ti"
  }
}

object EventType {
  case object Struct extends EventType

  case object Unstruct extends EventType

  case object PageView extends EventType

  case object PagePing extends EventType

  // Commented ADT are not implemented in generator
  //  case object StructLegacy extends EventType
  //  case object AdImpression extends EventType
  //  case object Transaction extends EventType
  //  case object TransactionItem extends EventType

  val gen: Gen[EventType] = Gen.oneOf(Struct, Unstruct, PageView, PagePing
    //      AdImpression, Transaction, TransactionItem,
  )
}

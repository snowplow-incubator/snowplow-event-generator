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
package com.snowplowanalytics.snowplow.eventgen.protocol.contexts

import com.snowplowanalytics.snowplow.eventgen.protocol.SelfDescribingJsonGen

object AllContexts {

  val derivedContexts: List[SelfDescribingJsonGen] =
    List(
      UaParserContext,
      YauaaContext
    )

  // "sent" meaning not derived contexts
  val sentContexts: List[SelfDescribingJsonGen] =
    List(
      AnonymousIp,
      BrowserContext,
      CheckoutStep,
      ClientSession,
      ConsentDocument,
      ConsentWithdrawn,
      DesktopContext,
      DeviceInfo,
      EcommerceProduct,
      GeolocationContext,
      GoogleCookies,
      GooglePrivate,
      HttpCookie,
      HttpHeader,
      InstanceIdentityDocument,
      JavaContext,
      MediaPlayer,
//      MobileContext,
      OptimizelyState,
      OptimizelySummary,
      OptimizelyVariation,
      OptimizelyVisitor,
      OssContext,
      PrivacySandboxTopics,
      PushRegistrationEvent,
      RokuVideo,
      SegmentScreen,
      SessionContext,
      UserData,
      VimeoMeta
    )

  val all: List[SelfDescribingJsonGen] =
    (derivedContexts ::: sentContexts).sortBy(_.schemaKey.toSchemaUri)
}

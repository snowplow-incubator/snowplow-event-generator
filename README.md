# Snowplow Event Generator

[![Build Status][build-image]][build] 
[![License][license-image]][license]

Snowplow Event Generator is a tool for random events manifest generation.
These manifests could contain enriched events with pre-defined total volume, duplicates distribution and amount of contexts.

## Quickstart Core

Core is a scala library for generating snowplow events.
```
libraryDependencies += "com.snowplowanalytics" % "snowplow-event-generator-core" % "0.1.1" % Test
```

```scala

scala> import com.snowplowanalytics.snowplow.eventgen._
import com.snowplowanalytics.snowplow.eventgen._

scala> import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent
import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent

// Generate a Pair of Coollector Payload and List of events in Payload
// 1 - eventPerPayloadMin min number of events in payload
// 10 - eventPerPayloadMax max number of events in payload
scala> runGen(SdkEvent.genPair(1,10), new scala.util.Random())
val res0: (com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload, List[com.snowplowanalytics.snowplow.analytics.scalasdk.Event]) =
(
       #################################### NEW EVENT ####################################
       ############  ############  ############ QueryString ############  ############  ##########
       e=se&se_ca=se_ca_bGGwdg51zH&se_ac=se_ca_3cGQ39VyCy&se_la=se_ca_bFDoeybB5N&se_pr=se_ca_QBBUS3F0pj
       &se_va=2.1806073378160644E255&p=web&aid=aid_5cyTBHuwf3&tna=tna_gLAhEnI4Rl&tid=1000000
       &eid=fe773959-3a47-4e9f-9645-80e74a0eb9d6&dtm=-1693085107000&stm=-1693056326000
       &tz=Asia%2FRangoon&tv=go_1.2.3&duid=duid_7ZXhpWOs6d&nuid=59e1c99d-eb2d-4ea2-80b6-fa73de2028bb
       &tnuid=2c945614-722b-4a71-abe1-73b5d07ba96e&uid=uid_rnvz780Kup&vid=1000000&
        ...
//SdkEvent.genPairDup
//(natProb: Float, synProb: Float, natTotal: Int, synTotal: Int, eventPerPayloadMin: Int, eventPerPayloadMax: Int)
// 1F - All events are natural duplicates
// 0F - No Synthetic duplicates
// 1 - All natural duplicates are copies of the same event
// 0 - No Synthetic duplicates
// 1 - Min event per payload
// 1 - Max eventd per payload
scala> :paste
// Entering paste mode (ctrl-D to finish)
val rng = new scala.util.Random()
List(
  runGen(SdkEvent.genPairDup(1F, 0F, 1, 0, 1, 1), rng),
  runGen(SdkEvent.genPairDup(1F, 0F, 1, 0, 1, 1), rng), 
  runGen(SdkEvent.genPairDup(1F, 0F, 1, 0, 1, 1), rng)
   )
  .map(_._1) // Take collector payload
  .map(_.payload // Pick indo the payload
    // get few fields to show on the screen
    .map(event => (event.et.eid, event.app.aid)) 
  )


// Exiting paste mode, now interpreting.

val res2: List[List[(Option[java.util.UUID], Option[String])]] =
  List(
    List((Some(98f1ea70-866f-4ed1-b74e-61a8e1f2c58b),Some(aid_0mC9Eg4X9F))),
    List((Some(98f1ea70-866f-4ed1-b74e-61a8e1f2c58b),Some(aid_0mC9Eg4X9F))),
    List((Some(98f1ea70-866f-4ed1-b74e-61a8e1f2c58b),Some(aid_0mC9Eg4X9F)))
)



// Synthetic duplicates example
scala> :paste
// Entering paste mode (ctrl-D to finish)

val rng = new scala.util.Random()
List(
  runGen(SdkEvent.genPairDup(0F, 1F, 0, 1, 1, 1), rng),
  runGen(SdkEvent.genPairDup(0F, 1F, 0, 1, 1, 1), rng),
  runGen(SdkEvent.genPairDup(0F, 1F, 0, 1, 1, 1), rng)
)
  .map(_._1) // Take collector payload
  .map(_.payload // Pick indo the payload
    // get few fields to show on the screen
    .map(event => (event.et.eid, event.app.aid))
  )


// Exiting paste mode, now interpreting.



val res3: List[List[(Option[java.util.UUID], Option[String])]] = 
  List(
    List((Some(77cc2249-6d3f-49f6-9143-35c2ffc36e96),Some(aid_EnXJiWoiK6))),
    List((Some(77cc2249-6d3f-49f6-9143-35c2ffc36e96),Some(aid_iWpBIzTKom))),
    List((Some(77cc2249-6d3f-49f6-9143-35c2ffc36e96),Some(aid_zxWvypeqDO)))
  )

```

## Quickstart Sinks


Snowplow Event Generator is a JVM application packaged in docker:

```bash
$docker pull snowplow/snowplow-event-generator-sinks:0.1.1
```

It is configured with HOCON file:

```json
{
    // Seed for random generation.
    "seed": 1,
  
    // Number of collector payloads to generate. 
    // Total number of event get multiplied by eventPerPayload* setting. 
    "payloadsTotal": 1000,
  
    // Min number of events per payload.
    // Single event has a 1:1 odds of being send via GET or POST
    // Max number of events per payload.
    "eventPerPayloadMin": 1,
  
    // multiple events always send via POST
    "eventPerPayloadMax": 10,
  
    // Output collector payloads in Thrift format
    "withRaw": true,

    // Output enriched events in TSV format
    "withEnrichedTsv": true,

    // Output transformed enriched events in JSON format
    "withEnrichedJson": true,
  
    // Compress all output in GZIP  
    "compress": false,
  
    // Number of collector payloads per file
    "payloadsPerFile": 100,
  
    // Optional secition to generate duplicates
    "duplicates": {        
       // Probablity of full event duplicate float 0 to 1
       "natProb": 0.9,
      
       // Probablity of synthetic event duplicate (only eid) float 0 to 1
       "synProb": 0.9,

       // Pool size for the natural duplicates. 
       // Setting it to 1 means that same events got duplicated all the time.
       // If total number of events is less then pool size, it might not generate any duplicates. 
       // Note that it could duplicate synthetic duplicate, as natural duplication applied later.
       "natTotal": 10,
      
        // Pool size for the sythetic duplicates (same eid but different everything else) 
        // Setting it to 1 means that same event ID got duplicated all the time.
        // If total number of events is less then pool size, it might not generate any duplicates.
       "synTotal": 1
}
```

And finally you can run it:

```bash
$ docker run snowplow/snowplow-event-generator-sinks:0.1.1 --config config/config.hocon.sample --output file:/tmp/out
$ docker run snowplow/snowplow-event-generator-sinks:0.1.1 --config config/config.hocon.sample --output s3://mybucket/out

```

## Copyright and License

Snowplow Events Generator is copyright 2021-2022 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[build]: https://github.com/snowplow-incubator/snowplow-event-generator/actions?query=workflow%3A%22Test+and+deploy%22
[build-image]: https://github.com/snowplow-incubator/snowplow-event-generator/workflows/Test%20and%20deploy/badge.svg


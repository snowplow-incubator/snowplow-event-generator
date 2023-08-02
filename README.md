# Snowplow Event Generator

[![Build Status][build-image]][build] 
[![License][license-image]][license]

Snowplow Event Generator is a tool for random events manifest generation.
These manifests could contain enriched events with pre-defined total volume, duplicates distribution and amount of contexts.
We provide [a command line tool](#quickstart-cli) to output events to a file, and also a [scala library](#quickstart-core) to support generating random events in unit tests.

## Quickstart CLI

Download the [executable from github](https://github.com/snowplow-incubator/snowplow-event-generator/releases/download/0.4.0/snowplow-event-generator-0.4.0.tar.gz). You need java installed to run it.

```bash
wget https://github.com/snowplow-incubator/snowplow-event-generator/releases/download/0.4.0/snowplow-event-generator-0.4.0.tar.gz
tar -xzf snowplow-event-generator-0.4.0.tar.gz
```

And start generating events in your `my-events` directory, in configuration file set:
```
"output": {
  "file": {
    "path": "file:/path/to/my-events"
  }
}
```

Alternatively you can write events directly to a S3 bucket:

```
"output": {
  "file": {
    "path": "s3://my-bucket/my-events"
  }
}
```

...or directly to a Kinesis stream:

```
"output": {
  "kinesis": {
    "streamName": "my-stream",
    "region": "eu-central-1"
  }
}
```


...or directly to a Pubsub topic:


```
"output": {
  "pubsub": {
    "subscription": "projects/my-project/topics/my-topic"
  }
}
```

...or directly to a Kafka topic:

```
"output": {
  "kafka": {
    "brokers": "my-broker:9092,my-broker-2:9092",
    "topic": "my-topic"
  }
}
```

...or directly to an EventHubs topic:

```
"output": {
  "kafka": {
    "brokers": "PLACEHOLDER.servicebus.windows.net:443",
    "topic": "enriched-topic",
    "producerConfig": {
    "security.protocol": "SASL_SSL",
    "sasl.mechanism": "PLAIN",
    "sasl.jaas.config": "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"Endpoint=sb://PLACEHOLDER.servicebus.windows.net/;SharedAccessKeyName=enriched-topic-read-write;SharedAccessKey=PLACEHOLDER;EntityPath=enriched-topic\";"
    }
  }
}
```

Then run:
```bash
./snowplow-event-generator --config my-config.hocon
```

By default, it generates 1000 events with no duplicates. The generated events are _deterministic_, which means if you re-run the app multiple times with the same configuration then you will generate the same events each time.

#### Configuration

The app can be configured by providing a HOCON file, like [the one in this example](./config/config.example.hocon):

```bash
./snowplow-event-generator --config /path/to/config.hocon
```

Generator will print following output, which could be used for verification:
Total number of payloads, events and count for each generated self-described json.
```
Payloads = 100
Events = 506
changeFormGenCount  = 771
clientSessionGenCount  = 388
consentDocumentCount  = 439
desktopContextCount  = 439
httpCookieCount  = 439
httpHeaderCount  = 439
googleCookiesCount  = 439
googlePrivateCount  = 439
optimizelyVisitorCount  = 439
optimizelyStateCount  = 439
optimizelyVariationCount  = 439
optimizelySummaryCount  = 439
sessionContextCount  = 439
consentWithdrawnCount  = 439
segmentScreenCount  = 439
pushRegistrationCount  = 439
uaParserContextCount  = 439
unstuctEventCount = 140
```

Aside from "output" configuration, all fields in the configuration file are optional:


```
{
    // Seed for random generation. Change the seed to generate a different set of events
    "seed": 1

    // Generate a random seed on initialisation. If true "seed" will be ignored, and a random integer will be used for seed.
    "randomisedSeed": false

    // Number of collector payloads to generate. 
    // Total number of event get multiplied by eventPerPayload* setting. 
    "payloadsTotal": 1000
  
    // Min number of events per payload.
    // Single event has a 1:1 odds of being send via GET or POST
    "eventPerPayloadMin": 1
  
    // Max number of events per payload.
    // Multiple events always send via POST
    "eventPerPayloadMax": 10
  
    // Output collector payloads to a "raw" sub-directory in base64-encoded Thrift format
    "withRaw": true

    // Output events to the "enriched" sub-directory in TSV format
    "withEnrichedTsv": true

    // Output events to the "transformed" sub-directory in JSON format
    "withEnrichedJson": true
  
    // Compress all output in GZIP  
    "compress": false
  
    // Number of collector payloads per output file
    "payloadsPerFile": 100
  
    // Configure duplicate events
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

    // Generated event timestamps are close in time to this value.
    "timestamps": {

      // Should be "Fixed" to use a deterministic timestamp, or "Now" to always generate recent events
      "type": "Fixed"

      // Only used for "Fixed" timestamps.  Change this to generate more recent or more historic events.
      "at": "2022-02-01T01:01:01z"
    },

    // Required: Storage to sink generated events into
    // Currently only a single output at a time is supported
    "output": {
      "file": {
        // Generate files locally
        "uri": "file:/path/to/my-events"
        // Generate files into an S3 bucket
        //"uri": "s3://my-bucket/my-events"
      }
      // "kafka": {
        // Required: Seed brokers to use
        // "brokers": "my-broker:9092,my-other-broker:9092",
        // Required: Topic to deliver messages into
        // "topic": "my-topic",
        // Optional: Additional properties like authentication configuration
        // "producerConfig": {
        //  "additional-parameter": "value"
        // }
      // }
      // "kinesis": {
        // Required: Kinesis stream URI
        // "uri": "my-kinesis-stream",
        // Optional: Region where Kinesis stream runs
        // "region": "eu-central-1"
      // }
      // "pubsub": {
        // Required: PubSub stream URI
        // "uri": "pubsub://projects/my-project/topics/my-topic"
      // }
    }
}
```

## Quickstart Core

Core is a scala library for generating snowplow events.

```
libraryDependencies += "com.snowplowanalytics" % "snowplow-event-generator-core" % "0.4.0" % Test
```

```scala

scala> import com.snowplowanalytics.snowplow.eventgen._
import com.snowplowanalytics.snowplow.eventgen._

scala> import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent
import com.snowplowanalytics.snowplow.eventgen.enrich.SdkEvent

scala> val rng = new scala.util.Random(1)
val rng: scala.util.Random = scala.util.Random@1cca85d5

scala> val now = java.time.Instant.parse("2022-01-01T01:01:01z")
val now: java.time.Instant = 2022-01-01T01:01:01Z

// Generate a Pair of Collector Payload and List of events in Payload
// 1 - eventPerPayloadMin min number of events in payload
// 10 - eventPerPayloadMax max number of events in payload
scala> runGen(SdkEvent.genPair(1,10, now), rng)
val res0: (com.snowplowanalytics.snowplow.eventgen.collector.CollectorPayload, List[com.snowplowanalytics.snowplow.analytics.scalasdk.Event]) =
(
       #################################### NEW EVENT ####################################
       ############  ############  ############ QueryString ############  ############  ##########
       e=pp&url=http%3A%2F%2FqHFXCGj.net%3A1%2FMvdRGGMERtRzEX3&ua=Mozilla%2F5.0+%28compatible%3B+MSIE+9.0%3B+Windows+NT+6.0%3B+Trident%2F5.0%29
       &page=Do+amet+dolor+elit+sed+consectetur+ipsum+sit+adipiscing+lorem&refr=http%3A%2F%2Fwww.wvHtxQL.ru%3A1%2FpBXAZkzW8SMHNNh
       &fp=1000000&cookie=0&lang=-1&f_pdf=1&f_qt=0&f_realp=1&f_wma=0&f_dir=1&f_fla=1&f_java=1&f_gears=0
       &f_ag=0&cd=1&ds=9467x5732&cs=ISO-8859-16&vp=10000x1&pp_mix=1&pp_max=1&pp_m...

//SdkEvent.genPairDup
//(natProb: Float, synProb: Float, natTotal: Int, synTotal: Int, eventPerPayloadMin: Int, eventPerPayloadMax: Int, now: Instant)
// 1F - All events are natural duplicates
// 0F - No Synthetic duplicates
// 1 - All natural duplicates are copies of the same event
// 0 - No Synthetic duplicates
// 1 - Min event per payload
// 1 - Max eventd per payload
scala> :paste
// Entering paste mode (ctrl-D to finish)
List(
  runGen(SdkEvent.genPairDup(1F, 0F, 1, 0, 1, 1, now), rng),
  runGen(SdkEvent.genPairDup(1F, 0F, 1, 0, 1, 1, now), rng), 
  runGen(SdkEvent.genPairDup(1F, 0F, 1, 0, 1, 1, now), rng)
   )
  .map(_._1) // Take collector payload
  .map(_.payload // Pick indo the payload
    // get few fields to show on the screen
    .map(event => (event.et.eid, event.app.aid)) 
  )


// Exiting paste mode, now interpreting.

val res1: List[List[(Option[java.util.UUID], Option[String])]] = List(
    List((Some(98f1ea70-866f-4ed1-b74e-61a8e1f2c58b),Some(aid_0mC9Eg4X9F))),
    List((Some(98f1ea70-866f-4ed1-b74e-61a8e1f2c58b),Some(aid_0mC9Eg4X9F))),
    List((Some(98f1ea70-866f-4ed1-b74e-61a8e1f2c58b),Some(aid_0mC9Eg4X9F)))
  )


// Synthetic duplicates example
scala> :paste
// Entering paste mode (ctrl-D to finish)

List(
  runGen(SdkEvent.genPairDup(0F, 1F, 0, 1, 1, 1, now), rng),
  runGen(SdkEvent.genPairDup(0F, 1F, 0, 1, 1, 1, now), rng),
  runGen(SdkEvent.genPairDup(0F, 1F, 0, 1, 1, 1, now), rng)
)
  .map(_._1) // Take collector payload
  .map(_.payload // Pick indo the payload
    // get few fields to show on the screen
    .map(event => (event.et.eid, event.app.aid))
  )

// Exiting paste mode, now interpreting.

val res2: List[List[(Option[java.util.UUID], Option[String])]] =
  List(
    List((Some(77cc2249-6d3f-49f6-9143-35c2ffc36e96),Some(aid_orTrI8vI9D))),
    List((Some(77cc2249-6d3f-49f6-9143-35c2ffc36e96),Some(aid_8borEVQRrt))),
    List((Some(77cc2249-6d3f-49f6-9143-35c2ffc36e96),Some(aid_6zkeZ28aTr)))
  )
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

[build]: https://github.com/snowplow-incubator/snowplow-event-generator/actions?query=workflow%3A%22CI%22
[build-image]: https://github.com/snowplow-incubator/snowplow-event-generator/workflows/CI/badge.svg

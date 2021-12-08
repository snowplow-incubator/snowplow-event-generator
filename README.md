# Snowplow Event Generator

[![Build Status][build-image]][build] 
[![License][license-image]][license]

Snowplow Event Generator is a tool for random events manifest generation.
These manifests could contain enriched events with pre-defined total volume, duplicates distribution and amount of contexts.

## Quickstart


Snowplow Event Generator is a JVM application, you can download it from GitHub:

```bash
$ wget TODO
```
Or get a docker image
```base
$ docker pull snowplow/snowplow-event-generator-sinks
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
  
    // Compress all output in GZIP  
    "compress": false,
  
    // Seralization mode for Sdk Events either "json" or "tsv"
    "enrichFormat": "json",
  
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
$ ./snowplow-event-generator --config config.hocon --output file:/tmp/output
$ ./snowplow-event-generator --config config.hocon --output s3://bybucket/folder
```

## Copyright and License

Snowplow Events Generator is copyright 2021 Snowplow Analytics Ltd.

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


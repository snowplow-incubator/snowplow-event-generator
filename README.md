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

It is configured with HOCON file:

```json
{

}
```

And finally you can run it:

```bash
$ ./snowplow-event-generator --config config.hocon
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


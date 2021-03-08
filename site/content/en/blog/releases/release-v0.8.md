---
title: "Release v0.8.0"
linkTitle: "Release Notes - v0.8.0"
date: 2020-09-24
---

**Azkarra Streams v0.8.0 is now available!**

Below is a summary of the issues addressed in the 0.8.0 release of Azkarra Streams. 

## New Features
* [*48c856e](https://github.com/streamthoughts/azkarra-streams/commit/48c856e) feat(runtime): add exclude topics to WaitForSourceTopicsInterceptor (#83)
* [2922f0e](https://github.com/streamthoughts/azkarra-streams/commit/2922f0e) feat(runtime): add timeout option to WaitForSourceTopicsInterceptor (#82)
* [c8af92c](https://github.com/streamthoughts/azkarra-streams/commit/c8af92c) feat(runtime): update MonitoringStreamsInterceptor to report state offset lags (#103)
* [f7eae4e](https://github.com/streamthoughts/azkarra-streams/commit/f7eae4e) feat(runtime): add new KafkaBrokerReadyInterceptor (#51)
* [a0e7cbd](https://github.com/streamthoughts/azkarra-streams/commit/a0e7cbd) feat(api/server): add support for HTTP Server-Sent Event (#100)
* [d19949f](https://github.com/streamthoughts/azkarra-streams/commit/d19949f) feat(server): allow to only return successfull records from IQ
* [d9a559d](https://github.com/streamthoughts/azkarra-streams/commit/d9a559d) feat(api): add ConfValue annotation

## Improvements and Bug fixes
* [a28ae57](https://github.com/streamthoughts/azkarra-streams/commit/a28ae57) refactor(api): add new params wait.for.topics.enable
* [d6feb38](https://github.com/streamthoughts/azkarra-streams/commit/d6feb38) refactor(runtime): remove AzkarraContextConfig class (#74)
* [d383da2](https://github.com/streamthoughts/azkarra-streams/commit/d383da2) fix(server): load AzkarraRestExtension using external component classLoaders
* [e80a4e5](https://github.com/streamthoughts/azkarra-streams/commit/e80a4e5) fix(api): exclude Kotlin packages during annotation scan (#107)
* [754651d](https://github.com/streamthoughts/azkarra-streams/commit/754651d) refactor(runtime): refactor AutoCreateTopicsInterceptor to use adminClient created from streams container
* [456f6c5](https://github.com/streamthoughts/azkarra-streams/commit/456f6c5) fix(server): fix invalid JSON return from GenericRecordSerializer
* [c32ccdb](https://github.com/streamthoughts/azkarra-streams/commit/c32ccdb) fix(server): fix JSON encoding for avro record with logical-type
* [dcda57a](https://github.com/streamthoughts/azkarra-streams/commit/dcda57a) refactor(api): clean and improve Annotation resolution

## Sub-Tasks
* [2f49f83](https://github.com/streamthoughts/azkarra-streams/commit/2f49f83) build(all): bump dependencies versions
* [dcdfe2e](https://github.com/streamthoughts/azkarra-streams/commit/dcdfe2e) refactor(streams): upgrade to Kafka Streams 2.6
* [63af93a](https://github.com/streamthoughts/azkarra-streams/commit/63af93a) refactor(streams): normalize the method returning Topology
* [04165df](https://github.com/streamthoughts/azkarra-streams/commit/04165df) build(maven): add wrapper for Maven 3.6.3 (#64)
* [05910a7](https://github.com/streamthoughts/azkarra-streams/commit/05910a7) refactor(api): extract interface from KafkaStreamsContainer
* [6d64b46](https://github.com/streamthoughts/azkarra-streams/commit/6d64b46) refactor(api/runtime): refactor creation of KafkaStreamsContainer
* [401648c](https://github.com/streamthoughts/azkarra-streams/commit/401648c) build(deps): bump log4j.version from 2.12.1 to 2.13.3
* [7cd68ce](https://github.com/streamthoughts/azkarra-streams/commit/7cd68ce) refactor(streams): deprecate ConfBuilder in favor to Conf
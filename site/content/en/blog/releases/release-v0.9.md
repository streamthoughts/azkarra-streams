---
title: "Release v0.9.0"
linkTitle: "Release Notes - v0.9.0"
date: 2021-03-03
---

**Azkarra Streams v0.9.0 is now available!**

Below is a summary of the issues addressed in the 0.9.0 release of Azkarra Streams. 

## New Features
* [689a9e3](https://github.com/streamthoughts/azkarra-streams/commit/689a9e3) feat(ui): enhance Azkarra UI to display the raw string topology description (#126)
* [3bdaadb](https://github.com/streamthoughts/azkarra-streams/commit/3bdaadb) feat(runtime): enhance MonitoringStreamsInterceptor to support pluggable reporters (#124)
* [9d844f3](https://github.com/streamthoughts/azkarra-streams/commit/9d844f3) feat(all): add new tab for displaying state store offsets and lags in Azkarra UI (#123)
* [1d9d3ad](https://github.com/streamthoughts/azkarra-streams/commit/1d9d3ad) feat(runtime): add a default StateRestoreListener to provide custom KafkaStreams states (#121)
* [20a7965](https://github.com/streamthoughts/azkarra-streams/commit/20a7965) feat(api/runtime): add new interface KafkaStreamsContainerAware (#120)
* [7fa4bce](https://github.com/streamthoughts/azkarra-streams/commit/7fa4bce) feat(all): fix and move AzkarraRocksDBConfigSetter to azkarra-commons module (#116)
* [d8980ef](https://github.com/streamthoughts/azkarra-streams/commit/d8980ef) feat(ui/client): enhance UI and fix azkarra-client build
* [3401c86](https://github.com/streamthoughts/azkarra-streams/commit/3401c86) feat(api): allow more fine-tuning of RocksDB instances (#116)
* [c6c8d64](https://github.com/streamthoughts/azkarra-streams/commit/c6c8d64) feat(streams): add StreamsConfigEntryLoader
* [eed9aa9](https://github.com/streamthoughts/azkarra-streams/commit/eed9aa9) feat(client): add Java Client for Azkarra
* [4f67fc6](https://github.com/streamthoughts/azkarra-streams/commit/4f67fc6) feat(all): refactor and add new endpoints for topologies
* [acb82f5](https://github.com/streamthoughts/azkarra-streams/commit/acb82f5) feat(api): allow to configure maxBlockingTime on EventStream

## Improvements and Bug fixes
* [f97bd55](https://github.com/streamthoughts/azkarra-streams/commit/f97bd55) fix(api): add missing method withBlockCacheSize to RocksDBConfig
* [ba57b14](https://github.com/streamthoughts/azkarra-streams/commit/ba57b14) refactor(api/runtime): set default environment name for LocalStreamsExecutionEnvironment
* [d63ad8e](https://github.com/streamthoughts/azkarra-streams/commit/d63ad8e) fix(commons): fix resources is not reallocated when stream is restarted
* [0a71b8e](https://github.com/streamthoughts/azkarra-streams/commit/0a71b8e) fix(commons): fix NPE LoggingStateRestoreListener
* [2064b6d](https://github.com/streamthoughts/azkarra-streams/commit/2064b6d) fix(runtime): fix producer already closed MonitoringReporter
* [b390c62](https://github.com/streamthoughts/azkarra-streams/commit/b390c62) fix(runtime): fix breaking change due to random container id
* [a5c5100](https://github.com/streamthoughts/azkarra-streams/commit/a5c5100) fix(api): remove deprecated class ConfBuilder
* [4bce06e](https://github.com/streamthoughts/azkarra-streams/commit/4bce06e) fix(ui): add a visited map to prevent duplicate node from showing up
* [471c3d1](https://github.com/streamthoughts/azkarra-streams/commit/471c3d1) fix(server): enforce content-type to JSON for ExceptionMapper
* [36f0bb0](https://github.com/streamthoughts/azkarra-streams/commit/36f0bb0) fix(server): fix Kafka Streams metric values should be returned with content-type text/plain (#125)
* [c4b80c4](https://github.com/streamthoughts/azkarra-streams/commit/c4b80c4) fix(ui): fix authentication modale does not show up
* [4ea170b](https://github.com/streamthoughts/azkarra-streams/commit/4ea170b) fix(streams): fix regression on server configuration
* [74a4502](https://github.com/streamthoughts/azkarra-streams/commit/74a4502) fix(api/runtime): fix IllegalStateException when providing KafkaStreamFactory (#119)
* [456ece2](https://github.com/streamthoughts/azkarra-streams/commit/456ece2) fix(metrics): fix missing @component on ConfigEntryLoader
* [a5c5100](https://github.com/streamthoughts/azkarra-streams/commit/a5c5100) fix(api): remove deprecated class ConfBuilder
* [781a800](https://github.com/streamthoughts/azkarra-streams/commit/781a800) fix(ui/runtime): cleanup UI and fix local container

## Sub-Tasks
* [0cc9862](https://github.com/streamthoughts/azkarra-streams/commit/0cc9862) deps(all): bump kafka streams version to 2.7.x #117
* [78ce9f4](https://github.com/streamthoughts/azkarra-streams/commit/78ce9f4) refactor(all): extract RestApiQueryCall
* [21ffbc0](https://github.com/streamthoughts/azkarra-streams/commit/21ffbc0) sub-taks(all): refactor StreamsExecutionEnvironment
* [554c4ae](https://github.com/streamthoughts/azkarra-streams/commit/554c4ae) sub-task(all): refator Azkarra API to support mutliple containers for a same application per environment
* [61a2d14](https://github.com/streamthoughts/azkarra-streams/commit/61a2d14) refactor(api): refactor API to query state stores
* [8259fe1](https://github.com/streamthoughts/azkarra-streams/commit/8259fe1) refactor(all): refactor StreamsExecutionEnvironment interface to support additional implementations
* [ae4b9be](https://github.com/streamthoughts/azkarra-streams/commit/ae4b9be) refactor(server): extract new interface InteractiveQueryService
* [12e1d78](https://github.com/streamthoughts/azkarra-streams/commit/12e1d78) refactor(all): extract interface ComponentScanner
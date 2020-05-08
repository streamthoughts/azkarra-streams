---
title: "Release v0.7.0"
linkTitle: "Release Notes - v0.7.0"
date: 2020-05-11
---

**Azkarra Streams v0.7.0 is now available!**

Below is a summary of the issues addressed in the 0.7.0 release of Azkarra Streams. 


## New Features
* [6e9e199](https://github.com/streamthoughts/azkarra-streams/commit/6e9e199) feat(metrics): add built-in support for Micrometer framework [#50](https://github.com/streamthoughts/azkarra-streams/issues/50)
* [2891108](https://github.com/streamthoughts/azkarra-streams/commit/2891108) feat(api): add the capability to eagerly initialize a component
* [d6c8eef](https://github.com/streamthoughts/azkarra-streams/commit/d6c8eef) feat(api): add support for conditional component.
* [899ecc8](https://github.com/streamthoughts/azkarra-streams/commit/899ecc8) feat(api): add support for Secondary component qualifier
* [1c6b7d4](https://github.com/streamthoughts/azkarra-streams/commit/1c6b7d4) feat(server): add support to register REST extensions [#63](https://github.com/streamthoughts/azkarra-streams/issues/63)
* [76146ee](https://github.com/streamthoughts/azkarra-streams/commit/76146ee) feat(server): add support to register custom jackson modules for KV records [#61](https://github.com/streamthoughts/azkarra-streams/issues/61)
* [22d1545](https://github.com/streamthoughts/azkarra-streams/commit/22d1545) feat(runtime): add new built-in interceptor MonitoringStreamsInterceptor [#52](https://github.com/streamthoughts/azkarra-streams/issues/52)
* [d9a12aa](https://github.com/streamthoughts/azkarra-streams/commit/d9a12aa) feat(ui): add Kafka Streams consumers progression bar [#58](https://github.com/streamthoughts/azkarra-streams/issues/58)
* [1b888e4](https://github.com/streamthoughts/azkarra-streams/commit/1b888e4) feat(ui): add new tabs to display consumer-lags [#58](https://github.com/streamthoughts/azkarra-streams/issues/58)
* [6cc3da7](https://github.com/streamthoughts/azkarra-streams/commit/6cc3da7) feat(api): add a default ConsumerInterceptor to track consumer consumed/committed offsets [#58](https://github.com/streamthoughts/azkarra-streams/issues/58)
* [22f4fba](https://github.com/streamthoughts/azkarra-streams/commit/22f4fba) feat(api): add new Primary annotation to qualify component
* [ac4d5c5](https://github.com/streamthoughts/azkarra-streams/commit/ac4d5c5) feat(runtime): allow to specify custom UncaughtExceptionHandler strategy [#55](https://github.com/streamthoughts/azkarra-streams/issues/55)

## Improvements and Bug fixes
* [6dbed7c](https://github.com/streamthoughts/azkarra-streams/commit/6dbed7c) refactor(metrics): make MeterRegistryConfigurer interface functional
* [beb0b65](https://github.com/streamthoughts/azkarra-streams/commit/beb0b65) refactor(runtime): add InvalidStreamsEnvironmentException when environment not found [#60](https://github.com/streamthoughts/azkarra-streams/issues/68)
* [f96426c](https://github.com/streamthoughts/azkarra-streams/commit/f96426c) fix(streams): fix some issues regarding components scan and creation
* [02ebc36](https://github.com/streamthoughts/azkarra-streams/commit/02ebc36) fix(runtime): fix bad delegating methods
* [b7ddf29](https://github.com/streamthoughts/azkarra-streams/commit/b7ddf29) fix(runtime): prevent topology to start twice when auto.start is enable [#59](https://github.com/streamthoughts/azkarra-streams/issues/59)
* [26e589a](https://github.com/streamthoughts/azkarra-streams/commit/26e589a) fix(server): health status must return DOWN when streams instance stop on failure [#54](https://github.com/streamthoughts/azkarra-streams/issues/54)

## Sub-Tasks
* [eb9d558](https://github.com/streamthoughts/azkarra-streams/commit/eb9d558) add support for Kafka Streams v2.5.0
* [ab47b99](https://github.com/streamthoughts/azkarra-streams/commit/ab47b99) refactor(server): externalize jackson serializers into dedicated module
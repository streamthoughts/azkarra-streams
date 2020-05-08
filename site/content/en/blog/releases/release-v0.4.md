---
title: "Release v0.4"
linkTitle: "Release Notes - v0.4"
date: 2019-12-02
---

**Azkarra Streams v0.4 is now available!**

Below is a summary of the issues addressed in the 0.4 release of Azkarra Streams. 

## Streams

**New Feature**

* [[AZKARRA-5](https://github.com/streamthoughts/azkarra-streams/issues/5)] - Add new option `enable.wait.for.topics` to wait for source topics to be created before starting Kafka Streams instance.

## API

**Improvement**

* [[AZKARRA-3](https://github.com/streamthoughts/azkarra-streams/issues/3)] - MapConf should support type conversion. This fixes a ClassCastException that was thrown when arguments were passed to the application (e.g --azkarra.server.port 8082).
## UI

**New Feature**

* [[AZKARRA-11](https://github.com/streamthoughts/azkarra-streams/issues/11)] - Add new option `server.enable.ui` for disable/enable Web UI.

**Improvement**

* [[AZKARRA-9](https://github.com/streamthoughts/azkarra-streams/issues/9)] - Prevent browser from opening auth popup when Basic authentication is configured.

**Bug**

* [Javascript] - Prevent the HTML form used to query state stores from being submitted.

## Security

**New Feature**

* [[AZKARRA-7](https://github.com/streamthoughts/azkarra-streams/issues/7)] - Add new option `rest.authentication.basic.silent` to enable silent basic authentication (default false).

**Improvement**

* [[AZKARRA-1](https://github.com/streamthoughts/azkarra-streams/issues/1)] - Obfuscate sensitive configuration (i.e password) returned in JSON HTTP response.

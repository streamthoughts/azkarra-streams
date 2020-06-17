---
date: 2020-05-09
title: "Maven modules"
linkTitle: "Maven modules"
weight: 20
description: >
  Information about the maven modules.
---

The Azkarra Sterams projects is organized around multiple maven modules:

* **azkarra-api** : The Azkarra Streams core API. This project contains the main Java interfaces and annotations (e.g `AzkarraContext`, `StreamsExecutionEnvironment`, `StreamsLifecicleInterceptor`, etc). 
* **azkarra-archetype** : The archetype project for Azkarra Streams.
* **azkarra-commons** :  This project provides miscellaneous reusable codes for Kafka Streams applications (i.e `DeserializationExceptionHandler`, `Serializer`, `Serdes`, etc). Azkarra Commons does not depend on other modules and can therefore be used directly by any Kafka Streams project.
* **azkarra-examples** : This project contains demo applications and code samples for Azkarra Streams API.
* **azkarra-json-serializers** : This project provides general JSON serializers for Azkarra Streams API, using Jackson API.
* **azkarra-metrics** : Azkarra Metrics. This project integrates Azkarra and Micrometer allowing metrics collection for Azkarra applications.
* **azkarra-runtime** : Azkarra Runtime. This project implements Azkarra API.
* **azkarra-server** : The Azkarra Streams embedded web server.
* **azkarra-streams** :	The main dependency for bootstrapping a new Azkarra Streams application. Azkarra Streams provides capabilities like component-scan, auto-configuration, etc.
* **azkarra-ui** : The Azkarra Dashboard. This module contains all the source code for main user interface of Azkarra Streams.

* **azkarra-worker** : The main distribution to install and deploy a single Azkarra Streams application as a standalone worker.
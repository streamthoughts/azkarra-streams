---
title: "FAQ"
linkTitle: "FAQ"
weight: 97
description: >
  The most frequently asked questions ?
---

### Could the Azkarra Streams library be used with another framework such as Spring, Quarkus or Micronaut?

Yes, this should be possible even if this is not what the Azkarra project was designed for. Azkarra is built around multiple Maven modules that can be imported individually. For example, **Azkarra Dashboard** is provided by the `azkarra-ui` module.

### What are the system dependencies of Azkarra Streams?

Like Kafka Streams, Azkarra Streams does not depend on any external system other than Apache Kafka.

### Which versions of Kafka Streams are supported by Azkarra Streams?

We try to always build the Azkarra Streams project with the latest available version of Kafka Streams.

|                             |  **Kafka Streams API (columns)**  |
|-----------------------------|:---------------------------------:|
|**Azkarra Version (rows)**   |	2.4 or prior versions             |
| v0.7 and earliers           | compatible                        | 
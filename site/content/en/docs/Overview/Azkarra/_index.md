---
date: 2020-05-09
title: "What is Azkarra ?"
linkTitle: "Azkarra"
weight: 10
description: >
  An introduction to Azkarra
---

## What is it?

**Azkarra Streams** is a lightweight java framework to make it easy to develop, deploy and manage streaming microservices based on Apache Kafka Streams.(Azkarra is Basque words meaning *"Fast"*).

{{% alert title="About Kafka Streams" color="info" %}}
**[Kafka Streams](https://kafka.apache.org/documentation/streams/)** is a client library for building applications and microservices, where the input and output data are stored in Kafka clusters. 
It combines the simplicity of writing and deploying standard Java and Scala applications on the client side  with the benefits of Kafka's server-side cluster technology (source: [Apache documentation](https://kafka.apache.org/documentation/streams/)).
{{% /alert %}}

### Key Features

Azkarra Streams provides a set of features to quickly debug and build production-ready Kafka Streams applications. This includes, among other things:

* Lifecycle management of Kafka Streams instances (no more KafkaStreams#start()).
* Easy externalization of configurations (using Typesafe Config).
* Embedded HTTP server to execute interactive queries.
* HTTP endpoints to monitor streams application metrics (e.g : JSON, Prometheus).
* Embedded Web user interface for the visualization of DAGs of topologies
* Encryption and Authentication with SSL or Basic Auth.
* Etc.


## Why do I want it?

Azkarra helps you build Kafka Streams applications using best pratices developped by the industry.

* **What is it good for?**: Azkarra lets you focus on writing Kafka Streams topologies code, not boilerplate code necessary for executing them.

* **What is it not good for?**: Azkarra is not attented to be used for operating a fleet of Kafka Streams applications.

* **What is it *not yet* good for?**: Azkarra cannot be used for managing a distributed Kafka Streams application.

## Where should I go next?

Give your users next steps from the Overview. For example:

* **Getting Started**: [Get started with Azkarra Streams](/azkarra-streams/docs/getting-started/)
* **Examples**: [Check out some example code!](/azkarra-streams/docs/examples/)






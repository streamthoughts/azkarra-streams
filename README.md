[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/streamthoughts/blob/master/LICENSE)
[![CircleCI](https://circleci.com/gh/streamthoughts/azkarra-streams.svg?style=svg&circle-token=dc27c1e59cfd3f4445d6cd234156773aae6e7013)](https://circleci.com/gh/streamthoughts/azkarra-streams)

![Logo of Azkarra Streams](images/azkarra-streams-logo.png)

Azkarra Streams is a lightweight Java framework which makes easy to develop and operate Kafka Streams applications (Azkarra is Basque word for *"Fast"*) 

_**[Kafka Streams](https://kafka.apache.org/documentation/streams/)** is a client library for building applications and microservices, where the input and output data are stored in Kafka clusters. 
It combines the simplicity of writing and deploying standard Java and Scala applications on the client side  with the benefits of Kafka's server-side cluster technology (source: [Apache documentation](https://kafka.apache.org/documentation/streams/))._

## 🚀 Features

* Create stand-alone Kafka Streams applications.
* Easy externalization of Topology and Kafka Streams configurations (using Typesafe Config).
* Embedded http server (Undertow).
* Embedded WebUI for topologies visualization.
* Provide production-ready features such as metrics, health checks, dead-letter-queues.
* Encryption and Authentication with SSL or Basic Auth.

## 🙏 Show your support

Do you think this project can help you create event-driven applications based on Kafka Streams?
Please 🌟 this repository to support us!

## 🚀 Quickstart 

Azkarra is available in [Maven Central](https://mvnrepository.com/artifact/io.streamthoughts/azkarra-streams-reactor/0.7.0). You can add Azkarra Streams to the dependency of the pom.xml of your project.

```xml
<dependency>
  <groupId>io.streamthoughts</groupId>
  <artifactId>azkarra-streams</artifactId>
  <version>0.8.0</version>
</dependency>
```

## Building Azkarra Streams

**Prerequisites for building Azkarra:**

* Git
* Maven (we recommend version 3.6.3)
* Java 11

```bash
$ git clone https://github.com/streamthoughts/azkarra-streams.git
$ cd azkarra-streams
$ ./mvnw clean package -DskipTests
```

*NOTE: Azkarra Worker is built in `./azkarra-worker/target/distribution/`*
    
## Documentation

If you want to read more about using **Azkarra Streams**, the documentation can be found on [www.azkarrastreams.io](https://www.azkarrastreams.io/)

## 💡Contributions

Any feedback, bug reports and PRs are greatly appreciated!

- Source Code: [https://github.com/streamthoughts/azkarra-streams](https://github.com/streamthoughts/azkarra-streams)
- Issue Tracker: [https://github.com/streamthoughts/azkarra-streams/issues](https://github.com/streamthoughts/azkarra-streams/issues)

## Community

* [Slack Channel](https://communityinviter.com/apps/azkarra-streams/azkarra-streams-community)

## Who Uses?

**Want to appear on this page?** 

Open an [issue](https://github.com/streamthoughts/azkarra-streams/issues) with a quick description of your organization and usage or send us a message to the [Slack Channel](https://communityinviter.com/apps/azkarra-streams/azkarra-streams-community).

## Licence

Copyright 2019-2020 StreamThoughts.

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License

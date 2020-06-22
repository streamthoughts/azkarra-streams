---
date: 2020-06-22
title: "Azkarra Worker"
linkTitle: "Azkarra Worker"
weight: 50
description: >
  How to run Azkarra Worker ?
---

## Downloading Azkarra Worker

```bash
AZKARRA_VERSION=0.7.3
wget https://github.com/streamthoughts/azkarra-streams/releases/download/v$AZKARRA_VERSION/azkarra-worker-$AZKARRA_VERSION.tar.gz -P .
tar -xzvf azkarra-worker-$AZKARRA_VERSION.tar.gz
cd azkarra-worker-$AZKARRA_VERSION
```

The installation contains the following structure : 

```bash
├── bin
│   ├── azkarra-streams-start.sh
│   └── azkarra-streams-stop.sh
├── etc
│   ├── azkarra.conf
│   └── log4j2.xml
├── LICENSE
├── README.md
└── share
    └── java
        └── azkarra-worker

```

## Running Worker

You can start a worker process with the following command : 

```bash
./bin/azkarra-streams-start.sh [-daemon]
```

## Configuring Worker

Azkarra uses the [Config](https://github.com/lightbend/config) library developed by Lightbend to 
ease the configuration of your application from an external file.

You can use the `-Dconfig.file` system property to specify the config source to load.

Example: 
```bash
STREAMS_JVM_OPTS=-Dconfig.file=./config/azkarra.conf ./bin/azkarra-streams-start.sh
```

Configs can also be passed passed to the command should be in the form of `[--property value]*`.
These parameters are then passed to the Azkarra main method.

Example: 

```bash
STREAMS_JVM_OPTS=-Dconfig.file=./config/azkarra.conf ./bin/azkarra-streams-start.sh \
--azkarra.component.paths /usr/share/azkarra-components
--azkarra.context.streams.bootstrap.servers localhost:9092
```

## External Components

To make your external components (e.g, `TopologyProvider`) available to the worker, installed them into one or many local directories.

Use the `azkarra.component.paths` property to configure the list of locations (separated by a comma) 
from which the components will be scanned.

Each configured directories may contain:

* an **uber JAR** containing all of the classes and third-party dependencies for the component (e.g., topology).
* a **directory** containing all JARs for the component.

## Using Docker

An official Docker image is available on [Docker Hub](https://hub.docker.com/r/streamthoughts/azkarra-streams-worker).

Example : 

```bash
docker run --net host streamthoughts/azkarra-streams-worker \
--mount type=bind,src=/tmp/azkarra/application.conf,dst=/etc/azkarra/azkarra.conf \
--mount type=bind,src=/tmp/components,dst=/usr/share/azkarra-components/ \
streamthoughts/azkarra-streams-worker
```
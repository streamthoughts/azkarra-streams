---
date: 2019-11-29
title: "Concepts"
linkTitle: "Concepts"
weight: 1
description: >
  Azkarra Concepts
---

This section describes the main concepts Azkarra uses to execute a Kafka Streams instance.

## Topology

The concept of `Topology` is not specific to Azkarra but is fundamental in the implementation of a Kafka Streams application. A Topology is an object that is part of the public Kafka Streams API and allows you to define all the operations (i.e stateless or stateful) to be performed on one or more Kafka topics.

In Azkarra Sterams, a `Topology` object must be defined and provided through the implementation of the `TopologyProvider` interface.


Here is a simple example : 


```java
    public class WordCountTopology implements TopologyProvider {

        @Override
        public Topology get() {
            StreamsBuilder builder = new StreamsBuilder();
            KStream<String, String> textLines = 
                          builder.stream("streams-plaintext-input");

            textLines
                .flatMapValues(value ->
                    Arrays.asList(value.toLowerCase().split("\\W+"))
                )
                .groupBy((key, value) -> value)
                .count(Materialized.as("WordCount"))
                .toStream()
                .to(
                   "streams-wordcount-output",
                   Produced.with(Serdes.String(), Serdes.Long())
                );
            return builder.build();
        }

        @Override
        public String version() {
            return "1.0";
        }
    }
```

One of the particularities in Azkarra, is that the `TopologyProvider` interface enforces you to provide a non-null topology version. 
Usually, you will return the version of your project (e.g : 1.0, 1.2-SNAPSHOT, etc).
Azkarra uses this version to generate a meaningful config `application.id` for your streams instance if no one is provided at runtime.


## Component

In Azkarra, any object that forms your streams application (e.g: `TopologyProviders`) and is registered to and managed by an `AzkarraContext` instance is called a **component**.  

More generally, a component is an object that is instantiated, assembled, and configured by a  `AzkarraContext` instance.

The concepts of component enable the implementation of the [dependency injection](./dependency-injection) pattern in Azkarra.


## StreamsExecutionEnvironment

Azkarra uses the concept of `StreamExecutionEnvironment` to safely execute your `Topologies`. Indeed, you are not anymore responsible to create and start a new `KafkaStreams` instance by yourself. This is done by the `StreamExecutionEnvironment`.

A `StreamExecutionEnvironment` is responsible for creating, configuring and starting a new `KafkaStreams` instance for each `Topology` you provide.

Here is a simple example to execute the topology we illustrated in the previous section.

```java
// (1) define your Kafka Streams configuration.
Map<String, Object> props = new HashMap<>(); 
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

// (2) create a new execution environment.
StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create(Conf.with("streams", props)); 

// (3) register the topology to run.
env.addTopology(WordCountTopology::new, Executed.as("wordcount"));

env.start(); // (4) start the KafkaStreams instance.
```

A `StreamExecutionEnvironment` instance can manage multiple `KafkaStreams` instances and you can also create multiple environments.

An environment allows you to define common configuration properties, listeners and behaviors to apply on a group of Kafka Streams instances

Moreover, an Azkarra application is not limited to a single environment. For example, if during the development phase you need to quickly test a Kafka Streams application on different Kafka clusters, you can create one `StreamsExecutionEnvironment` for each target Kafka Cluster.

In the code example above, you can notice that we name the topology to be executed. In the same way, we can also name an environment.

```
DefaultStreamsExecutionEnvironment.create(Conf.with("streams", props), "dev");
```

Azkarra uses this information to generate the `application.id` property to set for the Kafka Streams application. In our case, the generated id will be `dev-wordcount-1.0`.

## AzkarraContext

A `AzkarraContext` object is responsible for configuring and running one ore more `StreamsExecutionEnvironment`. 
In addition, the `AzkarraContext` allows you to quickly configure a Java shutdown hook to ensure that our JVM shutdowns are handled gracefully.


A context can be created as follows : 

```java
DefaultAzkarraContext.create(config)
    .addExecutionEnvironment(env)
    .setRegisterShutdownHook(true)
    .start();
```

 By default, an AzkarraContext will always create a default StreamsExecutionEnvironment named **__default**. This allows you to register the `TopologyProvider` directly at the context level.

```java
context.addTopology(WordCountTopologyProvider.class, Executed.as("wordcount"))
```

Registering a `TopoloyProvider` at the context level has some advantages. Indeed, this allows you to use the Java annotations provided by the Azkarra framework (e.g @TopologyInfo, @DefaultStreamsConfig). See [Configuration](/docs/configuration) for more information.


## AzkarraApplication

The `AzkarraApplication` is the top-level concept that is used to bootstrap an Azkarra Streams application.

Its main responsibility is to initialize an AzkarraContext using a user-defined configuration which describes the `StreamsExecutionEnvironment`s and the streams applications to be executed.

The AzkarraApplication is also responsible for deploying an embedded HTTP server (if enable) which exposes the REST endpoints that can be used to manage registered topologies and to run streams instances.

---
date: 2020-02-12
title: "TopologyProvider"
linkTitle: "TopologyProvider"
weight: 2
description: >
  The main interface to provide the Topology to be executed
---

The concept of `Topology` is not specific to Azkarra but is fundamental in the implementation of a Kafka Streams application. A Topology is an object that is part of the public Kafka Streams API and allows you to define all the operations (i.e stateless or stateful) to be performed on one or more input Kafka topics.

## 1.1 The `TopologyProvider` interface

In Azkarra Streams, a `Topology` object must be defined and provided through the implementation of the `TopologyProvider` interface.

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

One of the specificities of Azkarra is that the `TopologyProvider` interface requires you to provide a non-null topology version. 
Usually, you will return the version of your project (e.g: 1.0, 1.2-SNAPSHOT, etc).

{{% alert title="INFO" color="info" %}}
Azkarra uses the version to generate a meaningful config `application.id` for your streams instance when no one is provided at runtime.
{{% /alert %}}


{{% alert title="Limitation" color="warning" %}}
One of the current limitations of Azkarra is that `TopologyProvider` implementations must define no-arg constructor. 
{{% /alert %}}
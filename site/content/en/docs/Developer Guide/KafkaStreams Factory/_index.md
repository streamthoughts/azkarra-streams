---
date: 2020-01-06
title: "KafkaStreams Factory"
linkTitle: "KafkaStreams Factory"
weight: 5
description: >
---

By default, the Azkarra API is responsible for creating a new `KafkaStreams` instance for each provided `Topology`. 

But, in some cases, you may want to be able to customize how the `KafkaStreams` instances are created. 
For example, it may be to provide a `KafkaClientSupplier` that will add some tracing mechanisms on top of the Kafka clients (e.g: [kafka-opentracing](https://github.com/opentracing-contrib/java-kafka-client)

You can implement the `KafkaStreamsFactory` interface for that.

Below is the interface :  

```java
public interface KafkaStreamsFactory {

    KafkaStreams make(final Topology topology, final Conf streamsConfig);
}
```

If you wish to configure a `KafkaStreamsFactory` for a specific environment, then you can use the `StreamsExecutionEnvironment#setKafkaStreamsFactory` method.

```java
StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create();
env.setKafkaStreamsFactory(() -> new KafkaStreamsFactory() {
    @Override
    public KafkaStreams make(final Topology topology, final Conf streamsConfig) {
        KafkaClientSupplier clientSupplier = //...
        return new KafkaStreams(topology, streamsConfig.getConfAsProperties(), clientSupplier);
    }
});
```

In addition, a `KafkaStreamsFactory` can also be provided as a context component.

```java
@Component
public CustomKafkaStreamsFactory implements KafkaStreamsFactory {
  
    @Override
    public KafkaStreams make(final Topology topology, final Conf streamsConfig) {
        KafkaClientSupplier clientSupplier = //...
        return new KafkaStreams(topology, streamsConfig.getConfAsProperties(), clientSupplier);
    }
}
```

Like any other component, a `KafkaStreamsFactory` can implement the `Configurable` interface. 
The `Conf` object passed to the `configure()` method corresponds to the topology configuration.
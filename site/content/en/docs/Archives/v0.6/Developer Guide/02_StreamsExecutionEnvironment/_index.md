---
date: 2020-02-12
title: "StreamsExecutionEnvironment"
linkTitle: "StreamsExecutionEnvironment"
weight: 3
description: >
  The execution container that handles all the code logic to run and manage `KafkaStreams` instances.
---

## 2.1 Creating a new `StreamsExecutionEnvironment`

A `StreamExecutionEnvironment` is the interface for creating, configuring and starting new `KafkaStreams` instances. Basically, a `StreamExecutionEnvironment` is an execution container that handles all the code logic to run and manage your `Topology` objects.

Azkarra provides the default implementation `DefaultStreamsExecutionEnvironment` that you can instantiate as follows : 

```java
StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create()
``` 

{{% alert title="NOTE" color="info" %}}
Using Azkarra Streams, you no longer need to call the `KafkaStreams#start()` method.
{{% /alert %}}

## 2.2 Configuring a `StreamsExecutionEnvironment`

Each `StreamsExecutionEnvironment` can be configured with any property of your choice.
The configuration can be passed direcly while creating a new instance: 

```java
Map<String, Object> props = new HashMap<>(); 
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass()); (1)

Conf streamsConfig = Conf.with("streams", props) (2)

StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create(streamsConfig); (3)
```

or using the `setConfiguration` method: 

```java
env.setConfiguration(Conf.with("streams", props)) (4)
```

1. Create the `Map` to be used for configuring the `StreamsExecutionEnvironment`.
2. The streams properties must be prefixed with `streams.`.
3. The configuration is passed to `create` method.
4. Optionally, the configuration is setted using the `setConfiguration` method.


## 2.3 Registering `TopologyProvider`

You can register multiple `TopologyProvider` to an `StreamsExecutionEnvironment` using the `addTopology` method.

```java
env.addTopology(
    WordCountTopology::new,   (1)
    Executed.as("wordcount")  (2)
    );
```

1. The `TopologyProvider` accepts a `Supplier<TopologyProvider>` object.
2. The `Executed` class can be used to name and describe a `Topology`. For example, the name can be used to auto-generate an `application.id` if no one is configured.

In addition, the `Executed` class allows overriding environment properties : 

```java
env.addTopology(
    WordCountTopology::new,
    Executed.as("wordcount")
            .withDescription("Basic WordCount Topology")
            .withConfig(Conf.with("streams.application.id", "my-word-count-application"))
    );
``` 

## 2.4 Using multiple environments

A `StreamsExecutionEnvironment` allows setting common configuration properties, listeners and behaviors to a set of `KafkaStreams` instances.

For most usages, you will usually only create a single `StreamsExecutionEnvironment` instance. But, there is no restriction on the number of instances you can create. Having multiple `StreamsExecutionEnvironment` can be useful. 

For example, you may wish to execute a `KafkaStreams` application on two distinct Apache Kafka clusters. An `StreamsExecutionEnvironment` is uniquely identified by its name.

```java
StreamsExecutionEnvironment env1 = DefaultStreamsExecutionEnvironment.create(
    Conf.with("streams", confEnv1), "prod-eu-west-1");

StreamsExecutionEnvironment env2 = DefaultStreamsExecutionEnvironment.create(
    Conf.with("streams", confEnv2), "prod-eu-west-2");
```


{{% alert title="INFO" color="info" %}}
Azkarra uses the environment name to auto-generate the `application.id` property of `KafkaStreams` instance when no one is configured.
{{% /alert %}}

## 2.5 Automatically generating the `application.id` property

The `ApplicationIdBuilder` is the interface that can be used to automatically generate the `application.id` property to be set to a `KafkaStreams` instance.

Azkarra provides the `DefaultApplicationIdBuilder` implementation that will generates an identifier only if no one is already configured.
The generated application is in the form: `<env_name>-<topology_name>-<topology-version>`.

It’s also possible to provide a custom `ApplicationIdBuilder` using the `StreamsExecutionEnvironment#setApplicationIdBuilder` method.

```java
env.setApplicationIdBuilder(() -> new ApplicationIdBuilder() {
    @Override
    public ApplicationId buildApplicationId(final TopologyMetadata metadata, final Conf streamsConfig) {
        return new ApplicationId(metadata.name() + metadata.version());
    }
});
```

## 2.6 Customizing `KafkaStreams` instance

By default, Azkarra is responsible for creating a new `KafkaStreams` instance for each provided `Topology`. 

But, in some cases, you may want to be able to customize how the `KafkaStreams` instances are created. 

For example, it may be to provide a `KafkaClientSupplier` that will add some tracing mechanisms on top of the Kafka clients (e.g: [kafka-opentracing](https://github.com/opentracing-contrib/java-kafka-client).

Azkarra provides the `KafkaStreamsFactory` interface, allowing you to customize how `KafkaStreams` instances are built.

Below is the interface :  

```java
public interface KafkaStreamsFactory {

    KafkaStreams make(final Topology topology, final Conf streamsConfig);
}
```

To configure a `KafkaStreamsFactory` for a specific environment you can use the `StreamsExecutionEnvironment#setKafkaStreamsFactory` method.

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

## 2.7 The `StreamsExecutionEnvironmentAware` interface

Azkarra provides the `StreamsExecutionEnvironmentAware` interface that components can implement to be notified of the `StreamsExecutionEnvironment` that it runs in.

Below are the list of components that currenlty support the `StreamsExecutionEnvironmentAware` interface:

* `TopologyProvider`
* `ApplicationIdBuilder`
* `KafkaStreamsFactory`
* `StreamsLifecycleInterceptor`

## 2.9 Starting an environment

Most of the time, you will never have to start an environment directly because this will be managed by the `AzkarraContext` or `AzkarraApplication` classes.

But you may have to integrate Azkarra with another framework (such as Spring, Micronaut). And, because this is usually done at the `StreamsExecutionEnvironment` level it can be convenient to manually invoke the method bellow: 

```java
env.start();
```
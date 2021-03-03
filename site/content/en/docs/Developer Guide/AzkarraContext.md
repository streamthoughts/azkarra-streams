---
date: 2020-09-26
title: "AzkarraContext"
linkTitle: "AzkarraContext"
weight: 40
description: >

---

## 1 Creating a new `AzkarraContext`

The `AzkarraContext` is the main interface to configure, manage and run one ore more `StreamsExecutionEnvironment`. 

Azkarra provides the default implementation `DefaultAzkarraContext` that you can instantiate as follows : 

```java
AzkarraContext context = DefaultAzkarraContext.create()
``` 


{{% alert title="WARN" color="warning" %}}
You should ensure that only one `AzkarraContext` instance is created.
{{% /alert %}}


## 2 Configuring a `AzkarraContext`

The `AzkarraContext` can be configured using a `Conf` object.

The configuration can be passed directly while creating a new instance: 

```java
Map<String, Object> props = new HashMap<>(); 
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass()); (1)

Conf streamsConfig = Conf.of("streams", props) (2)

AzkarraContext context = DefaultAzkarraContext.create(streamsConfig); (3)
```

or using the `setConfiguration` method: 

```java
context.setConfiguration(Conf.of("streams", props)) (4)
```

1. Create the `Map` to be used for configuring the `context`.
2. The streams properties must be prefixed with `streams.`.
3. The configuration is passed to `create` method.
4. Optionally, the configuration is set using the `setConfiguration` method.

Note that all `StreamsExecutionEnvironment` registered into the `AzkarraContext` will automatically inherit its configuration.

## 3 Registering `StreamsExecutionEnvironment`

A `StreamsExecutionEnvironment` can be registered to the `AzkarraContext` using the `addExecutionEnvironment` method.

```java
context.addExecutionEnvironment(env);
```

By default, the `AzkarraContext` will always create a default `StreamsExecutionEnvironment` named **default**.

You can retrieve the default environment as follows:

```java
StreamsExecutionEnvironment defaultExecutionEnvironment = context.getDefaultEnvironment();
```

Note that all registered environments must have a distinct name.

## 4 Registering components

In Azkarra, any object that forms your streams application (e.g: `TopologyProviders`) and is registered to and managed by an `AzkarraContext` instance is called a **component**.  

More generally, a component is an object that is instantiated, assembled, and configured by an  `AzkarraContext` instance.

{{% alert title="Dependency Injection" color="primary" %}}
The concepts of component enable the implementation of the [dependency injection](/docs/developer-guide/dependencyinjection/) pattern in Azkarra.
{{% /alert %}}

```java
context.registerComponent(WordCountTopologyProvider.class);
```

## 5 Adding a `TopologyProvider`

A `TopologyProvider` can be registered directly at the context level.

```java
context
.addExecutionEnvironment(myEnv);
.addTopology(WordCountTopologyProvider.class, "my-env-name", Executed.as("wordcount"))
```

If no environment name is specified, then the `TopologyProvider` is added to the default `StreamsExecutionEnvironment`.
```java
context.addTopology(WordCountTopologyProvider.class, Executed.as("wordcount"));
```

As you can notice, here we are specifying the class of the `TopologyProvider` to be added instead of passing a `Supplier` as is done when registering a topology through the `StreamExecutionEnvironement.addTopology` method.

This is because the `AzkarraContext` manages a `TopologyProvider` as a component.

Registering a `TopoloyProvider` at the context level allows you to use the  annotations provided by the Azkarra framework (e.g @TopologyInfo, @DefaultStreamsConfig) for providing default metadata and streams configuration.


Currently, the `TopologyProvider` supports the following annotations :

 * `@TopologyInfo`: Set the topology description and custom alias.
 * `@ConfValue`: Set a default streams configuration (_repeatable_).
 

**Example:**

```java
@TopologyInfo( description = "Kafka Streams WordCount Demo", aliases = "custom")
@ConfValue(name = "streams." + StreamsConfig.NUM_STREAM_THREADS_CONFIG, value = "4")
@ConfValue(name = "streams." + StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, value = "2")
class MyCustomTopologyProvider implements TopologyProvider, Configureable {

    public void configure(final Conf conf) {
    }   
    
    public Topology topology() {
        return ...;
    }
}
```

You should note that annotations are only supported for topologies which are registered into an `AzkarraContext`.

## 6 Adding ShutdownHook

The `AzkarraContext` allows you to quickly configure a Java shutdown hook to ensure that our JVM shutdowns are handled gracefully.

```java
context.setRegisterShutdownHook(true);
```

## 7 Starting `AzkarraContext`

You can start all `StreamsExecutionEnvironment` registered into the `AzkarraContext` by invoking the `start()` method.

```java
context.start();
```


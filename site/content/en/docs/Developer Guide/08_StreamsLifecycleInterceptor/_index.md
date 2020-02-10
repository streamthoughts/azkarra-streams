---
date: 2020-02-12
title: "StreamsLifecycleInterceptor"
linkTitle: "The StreamsLifecycle Intercepting Chain"
weight: 7
description: >
  How to execute operations before/after a KafkaStreams instance starts/stops?
---

Azkarra maintains an *intercepting filter chain* internally to easily perform operations while starting or stopping a Kafka Streams instances by implementing and registering
`StreamsLifecycleInterceptor` instances.

Azkarra provides built-in interceptors for common operations like waiting for topics to be created before starting a streams instance.

## 8.1 The StreamsLifecycleInterceptor Interface

The `StreamsLifecycleInterceptor` interface defines two methods `onStart` and `onStop` that are respectively invoked
before the streams instance is started or before is stopped.

You should always take care to call `chain.execute()` to not break the chain.

```java
public interface StreamsLifecycleInterceptor {

    /**
     * Intercepts the streams instance before being started.
     */
    default void onStart(final StreamsLifecycleContext context, final StreamsLifecycleChain chain) {
        // code here is executed before the streams is being started.
        chain.execute();
        // code here is executed after the streams was started (successfully or not).
    }

    /**
     * Intercepts the streams instance before being stopped.
     */
    default void onStop(final StreamsLifecycleContext context, final StreamsLifecycleChain chain) {
        // code here is executed before the streams is being stopped.
        chain.execute();
        // code here is executed after the streams was stopped.
    }
}
```
The information about the current streams application, such as the application ID or the topology description,  can be retrieved from the `StreamsLifecycleContext` argument.
The `StreamsLifecycleContext` object can also be used for updating the current state of the Kafka Streams instance.

## 8.2 Registering an Interceptor

`StreamsLifecycleInterceptor` can be registered like any other components using the `registerComponent` methods that are exposed by
the `AzkarraContext` class or dynamically using the component-scan mechanism. 
The `AzkarraContext` will be responsible to add the registered interceptors to the `StreamsExecutionEnvironment`s and topologies.

The interceptors can also be directly add on a `StreamsExecutionEnvironment` level using the `addStreamsLifecycleInterceptor` method.
When, an interceptor is add to an environment, then it will be executed for all topologies running in that environment.

```java
env.addStreamsLifecycleInterceptor(() -> new MyCustomInterceptor());
```

Finally, interceptors can be defined per topology through the used of the  `Executed#withInterceptor` method.

```java
env.addTopology(
    ()-> new WordCountTopology(), 
    Executed.as("wordcount").withInterceptor(() -> new MyCustomInterceptor())
);
```

## 8.3 Configuring an Interceptor

Like any other component, a `StreamLifecycleInterceptor` can implement the `Configurable` interface. 
The `Conf` object passed to the `configure()` method corresponds to the topology configuration.

## 8.4 WaitForSourceTopicsInterceptor

When starting a new `KafkaStreams` instance, the application will fail while performing tasks assignment if one of the source topic is missing 
(error: INCOMPLETE_SOURCE_TOPIC_METADATA). 

To prevent from such error, Azkarra provides the built-in `WaitForSourceTopicsInterceptor` that block the KafkaStreams startup until all source topics are created.

The `WaitForSourceTopicsInterceptor` can be enable by setting the global application property `azkarra.context.enable.wait.for.topics` to true in
your *application.conf* file.

In addition, you can enable that interceptor per environment using the `StreamsExecutionEnvironment#setWaitForTopicsToBeCreated` method.

## 8.5 AutoCreateTopicsInterceptor

During the development phase, you may find yourself creating and deleting Kafka topics manually and before each run of your application.
To ease this operation, Azkarra provides the built-in `AutoCreateTopicsInterceptor` which can be used to automatically create the source and sink topics
before the streams application is started.

When enabled, the `AutoCreateTopicsInterceptor` is automatically configured by the `AzkarraContext`. 
The `AzkarraContext` will use the following properties to configure the `AutoCreateTopicsInterceptor`.

| Property                                | Type                | Description                                                         |
|-----------------------------------------|-------------------- |---------------------------------------------------------------------|
|  `auto.create.topics.enable`            |  boolean            | If `true`, creates all source and sink topics used by the topology. |
|  `auto.create.topics.num.partitions`    |  int                | The default number of partition.                                    |
|  `auto.create.topics.replication.factor`|  int                | The default replication factor.                                     |
|  `auto.create.topics.configs`           |  Map[string, string]| The configuration to be used for creating topics.                   |

You can also add and configure a `AutoCreateTopicsInterceptor` to a `StreamsExecutionEnvironment` instance : 
Here is a simple example : 

```java
StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create();
env.addStreamsLifecycleInterceptor( () -> {
    AutoCreateTopicsInterceptor interceptor = new AutoCreateTopicsInterceptor();
    interceptor.setReplicationFactor((short)3);
    interceptor.setNumPartitions(6);
    return interceptor;
});
```

### 8.5.1 Defining the list of Topics

By default, the `AutoCreateTopicsInterceptor` resolves the list of topics to be created from the `TopologyDescription` object.
But, you can also specify your own list of `NewTopic` to be created.

```java
env.addStreamsLifecycleInterceptor( () -> {
    AutoCreateTopicsInterceptor interceptor = new AutoCreateTopicsInterceptor();
    interceptor.setTopics(Collections.singletonList(
            new NewTopic("my-source-topic", 6, (short)3))
    );
    return interceptor;

});
```

When, the `AutoCreateTopicsInterceptor` is enable on context-level, the `AzkarraContext` will lookup for registered components of type `NewTopic`.
If you run multiple streams topologies (or environments) you can use the `@Restricted` annotation to specify the target environment or streams of the component.

Here is a simple example : 

```java
@Factory
public class TopicsFactory {

    @Component
    @Restricted(type = "streams", names = "wordCountTopology")
    public NewTopic sourceTopic() {
        return new NewTopic("my-source-topic", 6, (short)3);
    }
}
```

### 8.5.2 Automatically deleting topics

The `AutoCreateTopicsInterceptor` can also be used for automatically deleting any topics used by the topology when the streams instance is stopped.
Note: This property should be used with care and not enable for production.

| Property                                | Type                | Description                                                         |
|-----------------------------------------|-------------------- |---------------------------------------------------------------------|
|  `auto.delete.topics.enable`            |  boolean            | If `true`, deletes all topics after the streams is stopped (should only be used for development) |



---
date: 2021-03-03
title: "StreamsLifecycleInterceptor"
linkTitle: "The StreamsLifecycle Intercepting Chain"
weight: 70
description: >
  How to execute operations before/after a KafkaStreams instance starts/stops?
---

Azkarra maintains an *intercepting filter chain* internally to easily perform operations while starting or stopping a Kafka Streams instances by implementing and registering
`StreamsLifecycleInterceptor` instances.

Azkarra provides built-in interceptors for common operations like waiting for topics to be created before starting a streams instance.

## 1 The StreamsLifecycleInterceptor Interface

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
The information about the current stream application, such as the application ID or the topology description,  can be retrieved from the `StreamsLifecycleContext` argument.
The `StreamsLifecycleContext` object can also be used for updating the current state of the Kafka Streams instance.

## 2 Registering an Interceptor

`StreamsLifecycleInterceptor` can be registered like any other components using the `registerComponent` methods that are exposed by
the `AzkarraContext` class or dynamically using the component-scan mechanism. 
The `AzkarraContext` will be responsible to add the registered interceptors to the `StreamsExecutionEnvironment`s and topologies.

The interceptors can also be directly add on a `LocalStreamsExecutionEnvironment` level using the `addStreamsLifecycleInterceptor` method.
When, an interceptor is add to an environment, then it will be executed for all topologies running in that environment.

```java
env.addStreamsLifecycleInterceptor(() -> new MyCustomInterceptor());
```

Finally, interceptors can be defined per topology through the used of the  `Executed#withInterceptor` method.

```java
env.registerTopology(
    ()-> new WordCountTopology(), 
    Executed.as("wordcount").withInterceptor(() -> new MyCustomInterceptor())
);
```

## 3 Configuring an Interceptor

Like any other component, a `StreamLifecycleInterceptor` can implement the `Configurable` interface. 
The `Conf` object passed to the `configure()` method corresponds to the topology configuration.

## 4 WaitForSourceTopicsInterceptor

When starting a new `KafkaStreams` instance, the application will fail while performing tasks assignment if one of the source topic is missing 
(error: INCOMPLETE_SOURCE_TOPIC_METADATA). 

To prevent from such error, Azkarra provides the built-in `WaitForSourceTopicsInterceptor` that block the KafkaStreams startup until all source topics are created.

The `WaitForSourceTopicsInterceptor` can be enable by setting the global application property `azkarra.context.enable.wait.for.topics` to true in
your *application.conf* file.

In addition, you can enable that interceptor per environment using the `StreamsExecutionEnvironment#setWaitForTopicsToBeCreated` method.

### 4.1 Configuration properties

| Property                                | Type                | Description                                                         |
|-----------------------------------------|-------------------- |---------------------------------------------------------------------|
|  `wait.for.topics.timeout.enable`       |  boolean            | If `true`, enable and configure the interceptor. |
|  `wait.for.topics.timeout.ms`           |  boolean            | Wait until topics are created or this timeout is reached (Default is `LONG.MAX_VALUE`). |
|  `wait.for.topics.exclude.patterns`     |  list               | The list list of topics (regex) that the interceptor should not wait for (optional). |

## 5 AutoCreateTopicsInterceptor

During the development phase, you may find yourself creating and deleting Kafka topics manually and before each run of your application.
To ease this operation, Azkarra provides the built-in `AutoCreateTopicsInterceptor` which can be used to automatically create the source and sink topics
before the streams application is started.

When enabled, the `AutoCreateTopicsInterceptor` is automatically configured by the `AzkarraContext`. 
The `AzkarraContext` will use the following properties to configure the `AutoCreateTopicsInterceptor`.

### 5.1 Configuration properties

| Property                                | Type                | Description                                                         |
|-----------------------------------------|-------------------- |---------------------------------------------------------------------|
|  `auto.create.topics.enable`            |  boolean            | If `true`, creates all source and sink topics used by the topology. |
|  `auto.create.topics.num.partitions`    |  int                | The default number of partition.                                    |
|  `auto.create.topics.replication.factor`|  int                | The default replication factor.                                     |
|  `auto.create.topics.configs`           |  Map[string, string]| The configuration to be used for creating topics.                   |

You can also add and configure a `AutoCreateTopicsInterceptor` to a `StreamsExecutionEnvironment` instance : 
Here is a simple example : 

```java
LocalStreamsExecutionEnvironment
    .create()
    .addStreamsLifecycleInterceptor( () -> {
        AutoCreateTopicsInterceptor interceptor = new AutoCreateTopicsInterceptor();
        interceptor.setReplicationFactor((short)3);
        interceptor.setNumPartitions(6);
        return interceptor;
    });
```

### 5.2 Defining the list of Topics

By default, the `AutoCreateTopicsInterceptor` resolves the list of topics to be created from the `TopologyDescription` object.
But, you can also specify your own list of `NewTopic` to be created.

```java
LocalStreamsExecutionEnvironment
    .create()
    .addStreamsLifecycleInterceptor( () -> {
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

### 5.3 Automatically deleting topics

The `AutoCreateTopicsInterceptor` can also be used for automatically deleting any topics used by the topology when the streams instance is stopped.
Note: This property should be used with care and not enable for production.

| Property                                | Type                | Description                                                         |
|-----------------------------------------|-------------------- |---------------------------------------------------------------------|
|  `auto.delete.topics.enable`            |  boolean            | If `true`, deletes all topics after the streams is stopped (should only be used for development) |

## 6 MonitoringStreamsInterceptor

As of Azkarra v0.7.0, you can configure the built-in `MonitoringStreamsInterceptor` to periodically publish a state event of your `KafkaStreams` instance directly into a Kafka topic (default: `_azkarra-streams-monitoring`).

The `MonitoringStreamsInterceptor` can be enable by setting the global application property `azkarra.context.monitoring.streams.interceptor.enable` to true in
your *application.conf* file

### 6.1 The event format

Azkarra emits the state of `KafkaStreams` instances in the form of events that adhere to the [CloudEvents](https://cloudevents.io/) specification.

The CloudEvent specification is developed under the [Cloud Native Computing Foundation](https://cncf.io/) with the aim to describe a standardized and protocol-agnostic definition of the structure and metadata description of events. 

Currently, Azkarra only supports the [Structuted Content](https://github.com/cloudevents/spec/blob/v1.0/kafka-protocol-binding.md#33-structured-content-mode) mode for mapping CloudEvents to Kafka message. That means that the message-value contains event metadata and data together in a single envelope, encoded in JSON.

The following example shows a CloudEvent published by the `MonitoringStreamsInterceptor` using default configuration.

```json
{
  "id": "appid:basic-word-count-0-8-0;appsrv:localhost:8080;ts:1600951440046",   ① 
  "source": "arn://kafka=CFPFUjoZQnWcST8Dhh5Grw/host=localhost/port=8080",       ② 
  "subject": "arn://kafka=CFPFUjoZQnWcST8Dhh5Grw/host=localhost/port=8080/streams=basic-word-count-0-8-0",
  "specversion": "1.0",                                                  ③
  "type": "io.streamthoughts.azkarra.streams.stateupdateevent",          ④
  "time": "2020-05-08T22:13:39.636+0000",                                ⑤
  "datacontenttype": "application/json",                                 ⑥ 
  "ioazkarramonitorintervalms": 10000,                                   ⑦
  "ioazkarrastreamsappid": "basic-word-count",                           ⑧
  "ioazkarraversion": "0.8.0",                                           ⑨
  "ioazkarrastreamsappserver": "localhost:8082"                          ⑩
  "data": {                                                              ⑪
    "state": "RUNNING",
    "threads": [
      {
        "name": "basic-word-count-ab756b57-25ed-4c84-b4ef-93e9a84057ad-StreamThread-1",
        "state": "RUNNING",
        "active_tasks": [
          {
            "task_id": "0_0",
            "topic_partitions": [
              {
                "topic": "streams-plaintext-input",
                "partition": 0
              }
            ]
          },
          {
            "task_id": "1_0",
            "topic_partitions": [
              {
                "topic": "basic-word-count-count-repartition",
                "partition": 0
              }
            ]
          }
        ],
        "standby_tasks": [],
        "clients": {
          "admin_client_id": "basic-word-count-ab756b57-25ed-4c84-b4ef-93e9a84057ad-admin",
          "consumer_client_id": "basic-word-count-ab756b57-25ed-4c84-b4ef-93e9a84057ad-StreamThread-1-consumer",
          "producer_client_ids": [
            "basic-word-count-ab756b57-25ed-4c84-b4ef-93e9a84057ad-StreamThread-1-producer"
          ],
          "restore_consumer_client_id": "basic-word-count-ab756b57-25ed-4c84-b4ef-93e9a84057ad-StreamThread-1-restore-consumer"
        }
      }
    ],
    "offsets": {
      "group": "basic-word-count",
      "consumers": [
        {
          "client_id": "basic-word-count-ab756b57-25ed-4c84-b4ef-93e9a84057ad-StreamThread-1-consumer",
          "stream_thread": "basic-word-count-ab756b57-25ed-4c84-b4ef-93e9a84057ad-StreamThread-1",
          "positions": [
            {
              "topic": "streams-plaintext-input",
              "partition": 0,
              "consumed_offset": 10,
              "consumed_timestamp": 1588975991664,
              "committed_offset": 11,
              "committed_timestamp": 1588976019189,
              "log_end_offset": 11,
              "log_start_offset": 0,
              "lag": 0
            },
            {
              "topic": "basic-word-count-count-repartition",
              "partition": 0,
              "consumed_offset": 14,
              "consumed_timestamp": 1588975991664,
              "committed_offset": 15,
              "committed_timestamp": 1588976019209,
              "log_end_offset": 15,
              "log_start_offset": 15,
              "lag": 0
            }
          ]
        }
      ]
    },
    "stores": [
      {
        "name": "count",
        "positions": [
          {
            "partition": 0,
            "current_offset": 0,
            "log_end_offset": 0,
            "offset_lag": 0
          }
        ]
      }
    ],
    "state_changed_time": 1588975839528
  }
}
```

* **①** The unique id of the state change event, based on the : `application.id`, `application.server` and Unix epoch
* **②** The source of the event based on the: Kafka Cluster ID and `application.server` (note: arn=Azkarra Resource Name)
* **③** The CloudEvents specification versions
* **④** The type of change state event
* **⑤** Time of the state change or the event is emit
* **⑥** The content type of the data attribute; i.e JSON
* **⑦** The period the interceptor use to send a state change event for the current KafkaStream instance.
* **⑧** The `application.id` property value attached the KafkaStreams instance.
* **⑨** The version of Azkarra Streams
* **⑩** The `application.server` property value attached the KafkaStreams instance.
* **⑪** The actual state of KafkaStreams

You can also add your on CloudEvent extension attributes by configuring the property `monitoring.streams.interceptor.ce.extensions`.

{{% alert title="" color="info" %}}
Note : Configuration options, the CloudEvent fields and there semantics etc, may change in future releases, based on the feedback we receive from the Azkarra users.
{{% /alert %}}

### 6.2 Configuration properties

| Property                                | Type                | Description                                                         |
|-----------------------------------------|-------------------- |---------------------------------------------------------------------|
| `monitoring.streams.interceptor.enable`                  | boolean  | If `true`, enable and configure the interceptor |
| `monitoring.streams.interceptor.interval.ms`             | long     | The period the interceptor should use to send a streams state event (Default is 10 seconds).
| `monitoring.streams.interceptor.topic`                   | string   | The topic on which monitoring event will be sent (Default is `_azkarra-streams-monitoring`). |
| `monitoring.streams.interceptor.advertised.server`       | string   | The server name that will be included in monitoring events. If not specified, the streams `application.server` property is used. |
| `monitoring.streams.interceptor.ce.extensions`           | list     | The list of extension attributes that should be included in monitoring events. |
| `monitoring.streams.interceptor.info.enabled.stores.lag` | boolean  | If `true`, the interceptor will also report offset-lag for local state stores |
| `monitoring.streams.interceptor.reporters`               | list     | List of reporters to be used |
| `monitoring.streams.interceptor.kafka.reporter.enabled`  | boolean  | If `true` enable the default Kafka reporter |
## 6.3 MonitoringReporter

As of Azkarra v0.9.0, you can implement the interface `MonitoringReporter` to plug you own custom logic for reporting Kafka Stream metadata.

Reporters can be registered as standard components or through the configuration property `monitoring.streams.interceptor.reporters`.

Example: 
```java
@Component
public class ConsoleMonitoringReporter implements MonitoringRepoter {
    public void report(final KafkaStreamMetadata metadata) {
        System.out.println("Monitored KafkaStreams: " + metadata);
    }
}
```

## 7 KafkaBrokerReaderInterceptor

This interceptor waits for a number of broker to be available before starting the Kafka Streams application.

### 7.1 Configuration properties

| Property                                | Type                | Description                                                         |
|-----------------------------------------|-------------------- |---------------------------------------------------------------------|
| `kafka.ready.interceptor.enable`                 | boolean  | If `true`, enable and configure the interceptor |
| `kafka.ready.interceptor.timeout.ms`             | long     | Wait until brokers are available or this timeout is reached (Default is `60000`).
| `kafka.ready.interceptor.retry.backoff.ms`       | long     | The amount of time to wait before verifying that brokers are available (Default is `1000`). |
| `kafka.ready.interceptor.min.available.brokers`  | int      | The minimal number of broker that should be alive for the interceptor stops waiting (Default is `1`) |
---
date: 2021-03-11
title: "Azkarra Streams 0.9.0 release 🚀"
linkTitle: "Azkarra Streams 0.9.0 release"
description: ""
author: Florian Hussonnois ([@fhussonnois](https://twitter.com/fhussonnois))
resources:
- src: "**.{png,jpg}"
  title: "Image #:counter"
  params:
    byline: "Photo: Azkarra Streams Logo"
---

A few days ago we released [Azkarra](https://www.azkarrastreams.io/) Streams V0.9, and like every new release, it brings new features that make it easier to develop streams processing applications based on [Kafka Streams](https://kafka.apache.org/documentation/streams/). This version also represents a milestone since it includes important changes to Azkarra’s internal and public APIs in order to prepare the next steps of the project.

This blog post summarizes the most important improvements.

## Kafka Streams Memory Management

Kafka Streams uses [RocksDB](https://rocksdb.org/) for maintaining internal state stores. Usually, and depending on the number of states and the data throughput your application have to managed, it can be necessary to change the default settings of internal RocksDB instances. These can be specified through the rocksdb.config.setter configuration and the implementation of the RocksDBConfigSetter interface.

This release provides a new default [AzkarraRocksDBConfigSetter](https://www.azkarrastreams.io/docs/developer-guide/rocksdb/) class that allows advanced RocksDB tuning and helps to control the total memory usage across all instances.

For example, the configuraton below shows how to configure a block-cache size of 256MB shared across all RocksDB instances with a Write-Buffer-Manager of block.cache.size * 0.5.

```hocon
azkarra {
 streams {
  rocksdb.memory.managed = true
  rocksdb.memory.write.buffer.ratio = 0.5
  rocksdb.memory.high.prio.pool.ratio = 0.1
  rocksdb.block.cache.size = 268435456
 }
}
```

For more information about how to configure RocksDB you can follow the official [Tuning Guide](https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide).

## Monitoring State Store Recovery Process

Azkarra now automatically configures a LoggingStateRestoreListener which logs and captures the states of the restoration process for each store. In addition, those information are exposed through the REST endpoint GET /api/v1/streams/:id/stores :

Example (JSON response):

```json
[
  {
    "name": "Count",
    "partition_restore_infos": [
      {
        "starting_offset": 0,
        "ending_offset": 0,
        "total_restored": 0,
        "duration": "PT0.102756S",
        "partition": 0,
        "topic": "complex-word-count-topology-0-10-0-s-n-a-p-s-h-o-t-Count-changelog"
      }
    ],
    "partition_lag_infos": [
      {
        "partition": 0,
        "current_offset": 0,
        "log_end_offset": 0,
        "offset_lag": 0
      }
    ]
  }
]
```

## Exporting Kafka Streams States Anywhere

Since Azkarra v0.7, you can use the[StreamsLifecycleInterceptor](https://www.azkarrastreams.io/docs/developer-guide/streamslifecycleinterceptors/) so-called[MonitoringStreamsInterceptor](https://www.azkarrastreams.io/docs/developer-guide/streamslifecycleinterceptors/#86-monitoringstreamsinterceptor) that periodically publishes the state of your KafkaStreams instance, directly into a Kafka Topic, in the form of events that adhere to the [CloudEvents](https://cloudevents.io/) specification.

The [MonitoringStreamsInterceptor](https://www.azkarrastreams.io/docs/developer-guide/streamslifecycleinterceptors/#86-monitoringstreamsinterceptor) has been enhanced so that you can more easily report the states of your instances in third-party systems other than Kafka (e.g., DataDog). For this, Azkarra v0.9 introduces the new pluggable interface MonitoringReporter. Custom reporters can be configured through the monitoring.streams.interceptors.reporters property or declared as components.

Example:

```java
@Component
public class ConsoleMonitoringReporter implements MonitoringRepoter {

    public void report(final KafkaStreamMetadata metadata) {
        System.out.println("Monitored KafkaStreams: " + metadata);
    }
}
```


## Azkarra Dashboard

The UI of Azkarra Dashboard has been polished to provide a better user experience. In addition, a new tab has been added to get direct access to the state stores lag and offsets, as well as, information about their recovery process.


![Azkarra Streams — Dashboard —v0.9.0](/images/azkarra-streams-overview.gif)

## Azkarra API Changes

### LocalStreamsExecutionEnvironment

This new release makes some changes to the Azkarra Low-Level APIs, including the existing StreamsExecutionEnvironment interface. Specifically, the DefaultStreamsExecutionEnvironment has been replaced by newLocalStreamsExecutionEnvironment class which is used to run local KafkaStreams instances.

Example:

```java
LocalStreamsExecutionEnvironment
 .create("default", config)                                                
 .registerTopology(
        WordCountTopologyProvider::new,
        Executed.as("wordcount")
 ).start();
```

Currently, these changes do not directly impact applications developed with Azkarra. Indeed, they were motivated by the aim to bring in a future version additional implementations that will allow deploying and managing Azkarra instances running remotely, i.e., in Kubernetes :)

### Azkarra Client

Additionally, Azkarra 0.9 introduces a new module named azkarra-client that provides a simple Java Client API for Azkarra Streams, generated through the [OpenAPI Specification](https://swagger.io/specification/). Currently, the Client API is already used by Azkarra itself for executing remote Interactive Queries and will be leveraged in future versions to manage complete remote Azkarra instances.

**KafkaStreamsContainerAware**

Azkarra provides the new interface KafkaStreamsContainerAware that can be implemented by classes implementing :

* org.apache.kafka.streams.KafkaStreams.StateListener

* org.apache.kafka.streams.processor.StateRestoreListener

* io.streamthoughts.azkarra.api.streams.kafkaStreamsFactory

## Support for Apache Kafka 2.7

Finally, Azkarra Streams is always built on the most recent version of Kafka. Therefore, this new release adds support for version 2.7.

## Join the Azkarra Streams community on Slack

The Azkarra project has a dedicated Slack to help and discuss the community. [Join Us!](https://communityinviter.com/apps/azkarra-streams/azkarra-streams-community)

## Conclusion

I would like to thank you, the early adopters of Azkarra who, through their feedback and support, help the project to become more and more robust after each new version.

Please, share this article if you like this project. You can even add a ⭐ to the GitHub repository to support us.

Also, the project is open for contributions. So feel free to propose your ideas or project needs and of course to create pull requests (PR).

Thank you very much

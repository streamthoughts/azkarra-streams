---
date: 2020-02-12
title: "Configuration"
linkTitle: "Configuration"
weight: 2
description: >
  How to configure your application ?
---

This section describes how you can configure an Azkarra Application.

## 2.1 The `Configurable` interface

Azkarra allows any component registered into the `AzkarraContext` as well as the `TopologyProvider` to be configurable by implementing the interface `io.streamthoughts.azkarra.api.config.Configurable` : 

```java
public interface Configurable {
    void configure(final Conf configuration);
}
```

The `configure` method is automatically invoked after instantiating the `TopologyProvider`. 
The `Conf` instance passed as parameter contains the `StreamsExecutionEnvironment` configuration, in which the Topology has been registered, merged
with the `Conf` provided through the `Executed` object.

## 2.3 The `Conf` interface

The Azkarra project provides some built-in `Conf` implementations which are used internally.

* **ArgsConf** : A Conf implementation that can be created from an arguments array.
* **AzkarraConf** : A Conf implementation which is based on the Lightbend Config library.  
* **MapConf** : A Conf implementation that can be created from a Map.
* **Property** : A single key-value with configuration.

The `Conf` interface define methods : `getConfAsMap` and `getConfAsProperties` which be can used to easily creating a Consumer/Producer/KafkaStreams instance.
---
date: 2020-09-25
title: "Configuration"
linkTitle: "Configuration"
weight: 30
description: >
  How to configure your application ?
---

This section describes how you can configure an Azkarra Application.

## 1 The `Configurable` interface

Azkarra allows any component registered into the `AzkarraContext` as well as the `TopologyProvider` to be configurable by implementing the interface `io.streamthoughts.azkarra.api.config.Configurable` : 

```java
public interface Configurable {
    void configure(final Conf configuration);
}
```

The `configure` method is automatically invoked after instantiating the `TopologyProvider`. 
The `Conf` instance passed as parameter contains the `StreamsExecutionEnvironment` configuration, in which the Topology has been registered, merged
with the `Conf` provided through the `Executed` object.

## 2 The `Conf` interface

The Azkarra project provides some built-in `Conf` implementations which are used internally.

* **ArgsConf** : A Conf implementation that can be created from an arguments array.
* **AzkarraConf** : A Conf implementation which is based on the Lightbend Config library.  
* **MapConf** : A Conf implementation that can be created from a Map.
* **Property** : A single key-value with configuration.

The `Conf` interface define methods : `getConfAsMap` and `getConfAsProperties` which be can used to easily creating a Consumer/Producer/KafkaStreams instance.

## 3 The `@ConfValue` annotation

The `@ConfValue` annotation can be used to set default config values to a configurable component.

```java
@ConfValue("key", "value")
@Component
public MyComponent implements Configurable {
    public void configure(final Conf configuration);
}
```

## 3.1 `@ExactlyOnce`

Set the default stream property `processing.guarantee` to ` StreamsConfig.EXACTLY_ONCE`.

The `@ExactlyOnce` annotation must be used on a `TopologyProvider`.

## 3.2 `@AtLeastOnce`

Set the default stream property `processing.guarantee` to ` StreamsConfig.AT_LEAST_ONCE`.

The `@ExactlyOnce` annotation must be used on a `TopologyProvider`.
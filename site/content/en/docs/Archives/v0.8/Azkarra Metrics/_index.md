---
date: 2020-05-09
title: "Azkarra Metrics"
linkTitle: "Azkarra Metrics"
weight: 40
description: >
  Integration between Azkarra and Micrometer
---

This section describes how you can configure Azkarra Metrics.

## 1 Introduction

This project integrates Azkarra and [Micrometer](https://micrometer.io/) a metrics instrumentation library for JVM-based applications.

Azkarra Metrics is available since Azkarra Streams `0.7.0`.

## 2 Configuring

For using Azkarra Metrics, you will first have to include the `azkarra-metrics` dependency to your project.

* **Maven** : 
```xml
<dependency>
    <groupId>io.streamthoughts</groupId>
    <artifactId>azkarra-metrics</artifactId>
    <version>0.7.0</version>
</dependency>
```

If `component-scan` is enable, then Azkarra will automatically discover and configure all necessary components.

Then, Azkarra Metrics will only be enable if you configure the `azkarra.metrics.enable` property into you `application.conf` file:

**Property**: 

```hocon
azkarra {
  metrics {
    enable = true
  }
}
```

## 3 Micrometer Concepts

Micrometer defines several [concepts](https://micrometer.io/docs/concepts) in order to collect and expose measurements.

## 3.1 `MeterRegistry` and `Meter`

The two main concepts defines by Micrometer are :

* [`Meter`](http://micrometer.io/docs/concepts#_meters): The main interface for collecting individuals metrics. Micrometers packs with differents types of `Meter` (e.g `Timer`, `Counter`, `Gauge`, etc.). Each meter is uniquely identified by its name and dimensions.

* [`MeterRegistry`](http://micrometer.io/docs/concepts#_registry) : A `MeterRegistry` is the core component used to create and hold `Meters`. Depending on the registry that you used, meters can be hold in memory or be published in an external monitoring system (e.g: `Graphite`, `Elastic`, `Datadog`, etc).

Azkarra Streams builds a primary [`CompositeMeterRegistry`](http://micrometer.io/docs/concepts#_composite_registries) 
that will contain all `MeterRegistry` registered in AzkaraContext.

**Example using [GraphiteRegistry](http://micrometer.io/docs/registry/graphite):**

```java
@Factory
public class GraphiteMeterRegistryFactory implements Configureable {

	private Conf configs

    @Override
    public void configure(final Conf configs) {
        this.configs = configs;
    }

    @Component
    @Singleton
    @ConditionalOnMetricsEnable
    public FileDescriptorMetrics graphiteMeterRegistry() {
		GraphiteConfig graphiteConfig = new GraphiteConfig() {
		    @Override
		    public String host() {
		        return configs.getString("graphite.host");
		    }

		    @Override
		    public String get(String k) {
		        return null;
		    }
		};
		MeterRegistry registry = new GraphiteMeterRegistry(graphiteConfig, Clock.SYSTEM, HierarchicalNameMapper.DEFAULT)
    }
}
```

### 3.1.2 `MeterRegistryConfigurer` 

Azkarra allows you to automatically apply some customizations on registered `MeterRegistry`. 

Any component that implements the `MeterRegistryConfigurer` interface is applied on supported `MeterRegistry` during 
the `AzkarraContext` initialization.

**The interface**:

```java
/*
 * Copyright 2019 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.azkarra.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Interface that is used to configure a specific type of {@link MeterRegistry}.
 *
 * Any component that implements this interface will be applied on the supported {@link MeterRegistry} during
 * the {@link io.streamthoughts.azkarra.api.components.ComponentFactory} initialization.
 *
 * @see MeterRegistryFactory
 * @see MicrometerMeterRegistryConfigurer
 *
 * @param <T>   the meter registry type.
 */
@FunctionalInterface
public interface MeterRegistryConfigurer<T extends MeterRegistry> {

    /**
     * Applies this configurer to the given meter registry.
     *
     * @param meterRegistry the {@link MeterRegistry} to configure.
     */
    void apply(final T meterRegistry);

    /**
     * Checks whether this configurer supports the given meter registry type.
     *
     * @param meterRegistry a {@link MeterRegistry}.
     * @return boolean      {@code true} if the meter registry is supported.
     */
    default boolean supports(final T meterRegistry) {
        return true;
    }
}
```

**Example**:

```java
/**
 * Adds common tag `azkarra.version` on all meters.
 */
@Component
public class VersionTagRegistryConfigurer implements MeterRegistryConfigurer {

    @Override
    public void apply(final MeterRegistry meterRegistry) {
        meterRegistry.config().commonTags("azkarra.version", AzkarraVersion.getVersion());
    }
}
```    

## 3.2 `MeterFilter`

Micrometer allows you to configure meter filters on `MeterRegistry` in order to control over how and when meters are registered (see: [`MeterFilter`](https://micrometer.io/docs/concepts#_meter_filters)).

Any component that implements the `MeterFilter` interface will be applied to all registries.

```java
@Factory
public class MeterFilterFactory {

    /**
     * Deny all metrics starting with jvm.
     * @return {@link MeterFilter}
     */
    @Component
    @Singleton
    MeterFilter jvmDenyFilter() {
        return MeterFilter.denyNameStartsWith("jvm");
    }
```

## 3.3 `MeterBinder`

Micrometers provides several built-in [binders](https://github.com/micrometer-metrics/micrometer/tree/master/micrometer-core/src/main/java/io/micrometer/core/instrument/binder) to monitor the JVM, caches, ExecutorService, etc. 

Azkarra allows you to easily enable some of them through configuration.

### 3.3.1 Configuring binders

#### Kafka Streams

Azkarra Metrics provides the `MeterKafkaStreamsInterceptor` class that is responsible to configure and register the [KafkaStreamsMetrics](https://github.com/micrometer-metrics/micrometer/blob/master/micrometer-core/src/main/java/io/micrometer/core/instrument/binder/kafka/KafkaStreamsMetrics.java) binder for each running Kafka Streams instance. 

A tag `application_id` is added to each metrics.

`MeterKafkaStreamsInterceptor`, will only be enable if you configure the `metrics.binders.kafkastreams.enable` property.

**Property**: 

```hocon
azkarra {
  metrics {
    binders.kafkastreams.enable = true
  }
}
```

#### JVM

Azkarra Streams can also registered the [JVM metrics binders](https://micrometer.io/docs/ref/jvm) for you if you enable the `metrics.binders.jvm.enable` property.

The JVM metrics binders provides several JVM metrics on classloaders, memory, garbage collection, threads, etc.

**Property**:
```hocon
azkarra {
  metrics {
    binders.jvm.enable = true
  }
}
```

### 3.3.2 Adding custom binders

You can easely add a new binder by registering it as a component. `Azkarra Metrics` will automatically retrieve all registered components of type `MeterBinder`.

**Example:**
```java
/**
 * Get file descriptor metrics gathered by the JVM.
 **/
@Factory
public class FileDescriptorMetricsBinderFactory {

    @Component
    @Singleton
    @ConditionalOnMetricsEnable
    public FileDescriptorMetrics fileDescriptorMetrics() {
        return new FileDescriptorMetrics();
    }
}
```

## 4 Prometheus Endpoint

Azkarra Metrics provides a REST extension to expose all registered metrics via the `/prometheus` endpoint.


**Property**:
```hocon
azkarra {
  metrics {
    endpoints.prometheus.enable = true
  }

  server {
  	rest.extensions.enable = true
  }
}
```

N.B : Azkarra server must configured with REST extensions enable for the prometheus endpoints being accessible.


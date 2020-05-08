---
date: 2020-02-12
title: "Dependency Injection"
linkTitle: "Dependency Injection"
weight: 6
description: >
  How to use the dependency injection pattern in Azkarra Streams ?
---

The dependency injection pattern is a common practice in software 
development to get code that is modular and testable.

Azkarra does not implement any advanced dependency injection mechanism.
We think that you can leverage one of the existing IoC frameworks for achieving this.

However, Azkarra implements a simple mechanism based on the Supplier<T> interface 
for internally registering and getting concrete class instances.

You can easily use this mechanism to wire your services together.

## 6.1 Defining Components

When component-scan is enable, Azkarra will scan the current classpath and the configured external paths (i.e: `azkarra.component.paths`) to look up for classes
with `@Component` or `@Factory` annotations.

You can enable component-scan either programmatically : 

```java
new AzkarraApplication().setEnableComponentScan(true)  
```

or by annotate your main class with one of these annotations: `@AzkarraStreamsApplication` or `@ComponentScan`.

The following is a simple component example:

```java
@Component
@Singleton
public class ConfigurableStopWordsService implements StopWordsService, Configurable {

    private List<String> stopsWords;

    @Override
    public void configure(final Conf conf) {
        stopsWords = conf.getStringList("service.stopwords");
    }

    @Override
    public boolean test(final String word) {
        return stopsWords.contains(word.toLowerCase());
    }
}
```

Azkarra does not implement the [JSR-330 (javax.inject) - Dependency Injection for Java specification](http://javax-inject.github.io/javax-inject/) 
but leverages some of the defined Java annotations : 

* [`Singleton`](https://docs.oracle.com/javaee/6/api/javax/inject/Singleton.html)
* [`Named`](https://docs.oracle.com/javaee/6/api/javax/inject/Named.html)

Components are automatically registered to the `AzkrarraContext` which implement the interface `ComponentRegistry`.

Components can also be register programmatically : 

```java
AzkarraContext context = ...
context.registerComponent(ConfigurableStopWordsService.class);
```

or 

```java
context.registerSingleton(ConfigurableStopWordsService.class);
```

Note : A component is registered both for its type and all its supertypes.

A component instance can be retrieve directly from the `AzkarraContext` or the `ComponentFactory`.

```java
StopWordsService service = context.getComponent(StopWordsService.class);
```

The returned instance may be shared or independent, depending if it has been registered as a singleton or prototype.
In addition, if the component implements `Configurable`, then the new instance will be automatically configured using the context configuration.

Internally, Azkarra uses the `ComponentFactory` not only to automatically discover available Kafka Streams topologies
but also any classes that implement : 

* StreamsLifecycleInterceptor
* KafkaStreamsFactory
* HealthIndicator

This allows developers to easily extend Azkarra Streams features.

## 6.2 Qualifying By Name

By default, a component is named based on the name of its class.

To explicitly define the name of the component, you can use the [`Named`](https://docs.oracle.com/javaee/6/api/javax/inject/Named.html) annotation
at the class level.

```java
@Component
@Singleton
@Named("StopWordsService")
public class ConfigurableStopWordsService implements StopWordsService, Configurable {

    private List<String> stopsWords;

    @Override
    public void configure(final Conf conf) {
        stopsWords = conf.getStringList("service.stopwords");
    }

    @Override
    public boolean test(final String word) {
        return stopsWords.contains(word.toLowerCase());
    }
}
```

```java
StopWordsService service = context.getComponent(StopWordsService.class, Qualifiers.byName("StopWordsService"));
```

## 6.3 Component Version

A component is uniquely identify by a type, a name and optionally a version.

A component class that implements the `Versioned` interface can be qualified by the version return from the `Versioned:version` method.

For example, this is the case of the `TopologyProvider` interface.

```java
public interface Versioned {
    String version();
}
```

Currently, Azkarra supports the following version schema: 

`<major version>.<minor version>.<incremental version>-<qualifier>`.

Here are some valid examples : 

- `2`
- `2.0`
- `2.0.1`
- `2.0.1-SNAPSHOT`
- `2.0.1-rc3`

Moreover, Azkarra only supports a pre-defined subset of qualifiers: `alpha`, `beta`, `snapshot`, `rc` and `release`.

You can then get a component instance for a specific version.

```java
WordCountTopology topology = context.getComponent(WordCountTopology.class, Qualifiers.byVersion("1.0.0"));
```

or 

```java
WordCountTopology topology = context.getComponent(WordCountTopology.class, Qualifiers.byLatestVersion());
```

Note: A class that implement the `Versionned` interface must define a no-arg constructor. 

## 6.4 Component Factories

Defining a component either programmatically or using the `@Component` annotation is pretty straightforward.
However, this approach has some limitations. First, a class annotated with the `@Component` annotation must have a no-arg constructor in order
to be instantiable. Secondly, because you cannot annotated classes provided by third-party libraries, you cannot use the `@Component` annotation
to register as a component a class that is not part of your codebase.

For that, you can use a factory class.

A factory class is a simple Java class annotated with the `@Factory` annotation that provides one or more methods annotated with `@Component`.

```java
@Factory
public class TopicsFactory {

    @Component
    @Singleton
    public NewTopic streamsInputTopic() {
        return new NewTopic("streams-plaintext-input", 6, (short)1);
    }

    @Component
    @Singleton
    public NewTopic streamsOuputTopic() {
        return new NewTopic("streams-wordcount-output", 6, (short)1);
    }
}
```

For each method, Azkarra will create one proxy `Supplier` instance that delegate the component creation to 
one instance of the TopicsFactory.

Note : A factory class can implement the `Configurable` interface.

## 6.5 Component Suppliers

Another way to provide a component is to directly annotated a class implementing the [`Supplier`](https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html) interface with the `@Component` annotation.

```java
@Component
@Bean("simpleStopWordsService")
public class StopWordsServiceSupplier implements Supplier<StopWordsService>, Configurable {
    
    private Conf conf;

    @Override
    public void configure(final Conf conf) {
       this.conf = conf; 
    }

    @Override
    public StopWordsService get() {
        return new SimpleStopWordsService(conf.getStringList("service.stopwords"));
    }
}
``` 

## 6.6 Building the Graph

Azkarra does **NOT** support the [`@Inject`](https://docs.oracle.com/javaee/6/api/javax/inject/Inject.html) annotation specified by JSR-330 
to automatically linked components by their dependencies.

Developers are responsible for building the graph of objects programmatically by defining either a `@Factory` class or a `Supplier`
that implement the `ComponentFactoryAware` interface and `Configurable` (optionally).

For that purpose Azkarra provides the `ComponentModule` class.

The following is a simple example:

```java
@Component
public class ComplexWordCountTopologyModule extends ComponentModule<ComplexWordCountTopology> {

    @Override
    public ComplexWordCountTopology get() {
        StopWordsService service = getComponent(StopWordsService.class);
        ComplexWordCountTopology topology = new ComplexWordCountTopology();
        topology.setStopWordsService(service);
        return topology;
    }
}
```

## 6.7 Restricted Component

The `DefaultAzkarraContext` class will try to automatically configure streams environments and streams topologies using
the registered components. Specifically, the context looks for the components of type : 

* StreamsLifecycleInterceptor
* KafkaStreamsFactory
* NewTopic (when `auto.create.topics.enable` is `true`)

In many cases, you may want the `DefaultAzkarraContext` to only configure such component for specific environments or
streams topologies.

Azkarra defines the `@Restricted` annotation that can be used to limit/qualify the usage scope of a component.

A component can be restricted to : 

* one ore many specific streams environments (type = `env`).
* one ore many specific streams topologies (type = `streams`).
* the application (type = `application`).

For example, the following `KafkaStreamsFactory` will only be used for the streams application named *wordCountTopology*.

```java
@Component
@Restricted(type = Restriction.TYPE_STREAMS, names = "wordCountTopology")
public static class CustomKafkaStreamsFactory implements KafkaStreamsFactory {

    @Override
    public KafkaStreams make(final Topology topology, final Conf streamsConfig) {
        return new KafkaStreams(topology, streamsConfig.getConfAsProperties());
    }
}
```

Note: Defining a component with `@Restricted(type = Restriction.TYPE_APPLICATION)` annotation is equivalent to no annotation.

A complete code example is available on the GitHub project [azkarra-examples](https://github.com/streamthoughts/azkarra-streams/tree/master/azkarra-examples/src/main/java/io/streamthoughts/examples/azkarra/dependency)




---
layout: default
title: "Configuration"
parent: "Developer Guide"
nav_order: 2
---

# Configuration

## Configuring an Azkarra Application

The `AzkarraApplication` is responsible for configuring, and assembling 
the `AzkarraContext`, `StreamsExecutionEnvironment` and `TopologyProvider` instances.

For doing that, the `AzkarraApplication` is expecting specific properties :

**Context:**

| Property                             | Type                | Description                                                         |
|--------------------------------------|-------------------- |---------------------------------------------------------------------|
|  `azkarra.context`                   |  config             | The configuration of the `AzkarraContext`                           |                   
|  `azkarra.components`                |  Array[string]      | The list of components (e.g: `TopologyProvider`) to be registered into the `AzkarraContext`. |
|  `azkarra.environments`              |  Array[Environment] | The list of `StreamsExecutionEnvironment` to initialize and configure.   |
|  `azkarra.server`                    |  config | The configuration of the embedded HTTP server.                                  |

**Environment:** 

| Property                     | Type         | Description                                                         |
|----------------------------  |------------- |---------------------------------------------------------------------|
|  `name`                      |  string      | The environment name.                                               |     
|  `config`                    |  config      | The environment configuration.                                      |        
|  `jobs`                      |  Array[Job]  | The List of Kafka Streams jobs to run.                              |

**Streams/Topologies :**

| Property                     | Type         | Description                                                         |
|------------------------------|--------------|---------------------------------------------------------------------|
|  `name`                      |  string      | A short name which is used for identifying the streams job.         |     
|  `description`               |  string      | A optional description of the streams job.                          |     
|  `topology`                  |  string      | The fully qualified class name of the topology or an alias.         |                   
|  `config`                    |  config      | The props used to configure both `KafkaStreams` and `TopologyProvider` instances.  | 


Complete example (using HOCON format): 
```
azkarra {
  // The context configuration
  context {
    // The default configuration for streams application.
    streams {
      bootstrap.servers = "localhost:9092"
      default.key.serde = "org.apache.kafka.common.serialization.Serdes$StringSerde"
      default.value.serde = "org.apache.kafka.common.serialization.Serdes$StringSerde"
    }
  }

  // Manually defines the providers to be registered
  components = [
    io.streamthoughts.azkarra.example.topology.WordCountTopology
  ]

  // Create a default environment for running the WordCountTopology
  environments = [
    {
      name: "__default"
      jobs = [
        {
          name = "word-count-demo"
          description = "Kafka Streams WordCount Demo"
          topology =  "WordCountTopology"
          config = {}
        }
      ]
    }
  ]

  server {
    listener = localhost
    port = 8080
    headless = false
    // These information will be exposes through the http endpoint GET /info
    info {
      app {
        name = "@project.name@"
        description = "@project.description@"
        version = "@project.version@"
        encoding = "@project.build.sourceEncoding@"
        java.version = "@java.version@"
      }
    }
  }
}
```

Code snippet for auto-configuring an `AzkarraApplication` : 

```java
    var appConfig = ...;
    var application = new AzkarraApplication();
    application.configure(appConfig);
```

## Conf Implementations

The Azkarra project provides some built-in `Conf` implementations which are internally used.

* **ArgsConf** : A Conf implementation that can be created from an arguments array.
* **AzkarraConf** : A Conf implementation which is based on the Lightbend Config library.  
* **MapConf** : A Conf implementation that can be created from a Map.
* **Property** : A single key-value with configuration.

The `Conf` interface define methods : `getConfAsMap` and `getConfAsProperties` 
which be can used to easily creating a Consumer/Producer/KafkaStreams instance.

## Configuring components

Azkarra allows any component registered into the `AzkarraContext` as well as topology providers to be configurable.
For that, a component have to implement the interface `io.streamthoughts.azkarra.api.config.Configurable` : 

```java
public interface Configurable {
    void configure(final Conf configuration);
}
```

For components returned from method `AzkarraContext#getComponent()`, the method `configure` is automatically invoke
after instantiating the component. The `Conf` instance passed as parameter is the `AzkarraContext` configuration.

For `TopologyProvider`, the `configure` method is automatically invoked after instantiating the component. 
The `Conf` instance passed as parameter will contain the `StreamsExecutionEnvironment` configuration merged
with the configuration provided through the `Executed` object.

In addition, a `TopologyProvider` that wishes to be notified of the StreamsExecutionEnvironment that it runs in can 
implement the `StreamsExecutionEnvironmentAware`.

This can makes sense for example when an provider requires access to the environment name or configuration. 

## Component Scan

Azkarra provides a simple mechanism to search for components present in the classpath.
Any class annotated with `@Component` or implementing the interface `ComponentFactory` will be scanned and automatically
registered to the `AzkarraContext`.

The component scan can be enable programmatically through the method `AzkarraApplication#setEnableComponentScan`.
To automatically start all registered `TopologyProviver`, you must also enable auto-start.

Code snippet for enabling component scan : 

```java
   application
      .setEnableComponentScan(true)
      .setAutoStart(true) 
```

## Annotation Based Configuration

Another way to initialize and configure an `AzkarraApplication` is to use annotations.
Therefore, you can simply annotated you main class with `@AzkarraStreamsApplication` 
and initialize your `AzkarraApplication` as follows:

```java
@AzkarraStreamsApplication
public class SimpleStreamsApp {

    public static void main(final String[] args) {
        AzkarraApplication.run(SimpleStreamsApp.class, args);
    }
}
```

The `@AzkarraStreamsApplication` is a convenience annotation that adds all of the following:

 * `@EnableAutoConfig`: Tells Azkarra to auto-configure internal `AzkarraContext`
 * `@EnableAutoStart`: Tells Azkarra to automatically start any registered `TopologyProvider` using the default environment.
 * `@EnableEmbeddedHttpServer`: Tells Azkarra to start the embedded http-server.
 * `@ComponentScan`: Tells Azkarra to look for classes annotated with `@Component` in the current package.

In addition, Azkarra allows `TopologyProvider` to be annotated for providing default metadata and streams configuration.

Currently, `TopologyProvider` supports the following annotations :

 * `@TopologyInfo`: Set the topology description and custom alias.
 * `@DefaultStreamsConfig`: Set a default streams configuration (_repeatable_).
 
You should note that annotations are only supported for topologies which are registered into an `AzkarraContext`.

**Example**

```java
@TopologyInfo( description = "Kafka Streams WordCount Demo", aliases = "custom")
@DefaultStreamsConfig(name = StreamsConfig.NUM_STREAM_THREADS_CONFIG, value = "4")
@DefaultStreamsConfig(name = StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, value = "2")
class MyCustomTopologyProvider implements TopologyProvider, Configureable {

    public void configure(final Conf conf) {
        ...
    }   
    
    public Topology get() {
        return ...;
    }
}
```

---
date: 2020-02-12
title: "AzkarraApplication"
linkTitle: "AzkarraApplication"
weight: 5
description:
---

The `AzkarraApplication` is the top-level concept that is used to bootstrap an Azkarra Streams application.

Its main responsibility is to initialize an AzkarraContext using a user-defined configuration which describes the `StreamsExecutionEnvironment`s and the streams applications to be executed.

The AzkarraApplication is also responsible for deploying an embedded HTTP server (if enable) 

## 5.1 Creating a new `AzkarraApplication`

```java
AzkarraApplication application = new AzkarraApplication();
```

## 5.2 Auto-configuration application 

The `AzkarraApplication` allows you to automatically configure and assemble the internal `AzkarraContext`, `StreamsExecutionEnvironment` and `TopologyProvider` instances using the `AzkarraConf` class.


```java
AzkarraConf azkarraConf = AzkarraConf.create("application");
AzkarraApplication application = new AzkarraApplication()
        .setConfiguration(azkarraConf);
```

### 5.2.1 Configuring AzkarraContext

```hocon
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
  ...
}
```

### 5.2.2 Configuring components to register

Complete example (using HOCON format): 
```hocon
azkarra {
  ...	
  // Manually defines the providers to be registered
  components = [
    io.streamthoughts.azkarra.example.topology.WordCountTopology
  ]

}
```

### 5.2.3 Configuring `StreamsExecutionEnvironment` and `TopologyProvider`   

The `azkarra.environments` property allows you to list the `StreamsExecutionEnvironment` to initialize and configure.

Here are the properties to use: 

| Property                     | Type         | Description                                                                       |
|----------------------------  |------------- |-----------------------------------------------------------------------------------|
|  `name`                      |  string      | The name of the environment                                                       |     
|  `config`                    |  config      | The configuration of the environment                                              |        
|  `jobs`                      |  Array[Job]  | The list of `Topologyprovider` to add to execute in the environment (a.k.a *job*) |


Moreover, you can declared the list of `TopologyProvider` to be registered into a  `StreamsExecutionEnvironment` using the following properties:


| Property                     | Type         | Description                                                         |
|------------------------------|--------------|---------------------------------------------------------------------|
|  `name`                      |  string      | A short name which is used for identifying the streams job.         |     
|  `description`               |  string      | A optional description of the streams job.                          |     
|  `topology`                  |  string      | The fully qualified class name of the topology or an alias.         |                   
|  `config`                    |  config      | The props used to configure both `KafkaStreams` and `TopologyProvider` instances.  |


```hocon
azkarra {
  ...	
  // Create a default environment for running the WordCountTopology
  environments = [
    {
      name: "__default"
      config = {}
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
}
```


### 5.2.4 Configuring embedded http-server
```hocon
azkarra {
  ...	
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

## 5.3 Enabling component-scan

Azkarra provides a simple mechanism to search for components present in the classpath or in external directories.

Any class annotated with `@Component` or `@Factory` will be scanned and automatically registered to the `AzkarraContext`.

The component-scan can be enable programmatically through the method `AzkarraApplication#setEnableComponentScan`.
To automatically start all registered `TopologyProviver`, you must also enable auto-start.

Code snippet for enabling component scan : 

```java
application.setEnableComponentScan(true);
```

## 5.4 Auto-starting topologies

By enabling *auto-start* all `TopologyProvider`  that was registered during component-scanning will be automatically added to the given `StreamExecutionEnvironment`.

```java
application.setAutoStart(true, "__default");
```

## 5.5 Enabling embedded HTTP server

Azkarra packs with an embedded HTTP server (based on [Undertow](http://undertow.io/)) which exposes REST endpoints, allowing you to manage registered topologies and to run streams instances.

```java
application.enableHttpServer(true, HttpServerConf.with("localhost", 8080))
```

Note that when you enable the HTTP-server, Azkarra automatically configures the `application.server` property for running `KafkaStreams` instances.

## 5.6 The `@AzkarraStreamsApplication` annotation


Another way to initialize and configure an `AzkarraApplication` is to use annotations.
Therefore, you can simply annotated you main class with `@AzkarraStreamsApplication`  and initialize your `AzkarraApplication` as follows:

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
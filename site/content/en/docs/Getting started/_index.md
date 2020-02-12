---
date: 2020-01-06
title: "Getting Started"
linkTitle: "Getting Started"
weight: 2
description: >
  Get started with Azkarra Streams through a step by step tutorial.
---

In this tutorial we will explore the main Azkarra APIs step by step.
For that purpose we are going to develop a Kafka Streams application using the famous
 [WordCountTopology](https://kafka.apache.org/23/documentation/streams/quickstart) example.
 
The prerequisites for this tutorial are :

* IDE or Text editor.
* Java 11
* Maven 3+
* Docker (for running a Kafka Cluster 2.x).

## Setting up a Maven Project

We are going to use a Azkarra Streams Maven Archetype for creating a simple project structure.
First, run this following command : 
```bash
$> mvn archetype:generate -DarchetypeGroupId=io.streamthoughts \
-DarchetypeArtifactId=azkarra-quickstart-java \
-DarchetypeVersion=0.6.0 \
-DgroupId=azkarra.streams \
-DartifactId=azkarra-getting-started \
-Dversion=1.0-SNAPSHOT \
-Dpackage=azkarra \
-DinteractiveMode=false
```

Maven will create a new project with the following structure : 

```bash
$> tree azkarra-getting-started
 azkarra-getting-started
├── docker-compose.yml
├── pom.xml
├── quickstart-create-wordcount-topics.sh
└── src
    └── main
        ├── java
        │   └── azkarra
        │       ├── SimpleStreamsApp.java
        │       └── Version.java
        └── resources
            ├── application.conf
            ├── log4j2.xml
            └── version.properties
```

The `pom.xml` already contains the Azkarra Streams and Kafka Streams dependencies :

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-streams</artifactId>
        <version>2.4.0</version>
    </dependency>

    <dependency>
        <groupId>io.streamthoughts</groupId>
        <artifactId>azkarra-streams</artifactId>
        <version>0.5.0</version>
    </dependency>
</dependencies>
```

By default, Maven resources files are add to `src/main/resources`
The archetype contains a default configuration file `application.conf` that we can ignore for the moment. We will come back to this later.

The project also contains a very straightforward `docker-compose.yml` that we are going to use for setting up a single node Kafka cluster

## Writing Your First App

Using your favorite IDE or editor, open the Maven project. 
For more fun, we will remove the bundle example and start our first app from scratch.

```bash
$> cd azkarra-getting-started
$> rm -rf src/main/java/azkarra/*.java
```

First, let's create a new file `src/main/java/azkarra/SimpleStreamsApp.java` with a basic java `main(String[] args)` method.

```java
public class SimpleStreamsApp {
    public static void main(final String[] args) {
    }
}
```
In Azkarra, we have to implement the `TopologyProvider` interface in order to return a `Topology` instance through a method `get()`.
For our example, we will start by creating a new class named `WordCountTopologyProvider` which implements this interface.
To keep it simple, you can declare a nested class into `SimpleStreamsApp`.
 
```java
public static class WordCountTopologyProvider implements TopologyProvider {

    @Override
    public String version() { return "1.0-SNAPSHOT"; }

    @Override
    public Topology get() {
        final StreamsBuilder builder = new StreamsBuilder();

        final KStream<String, String> source = builder.stream("streams-plaintext-input");

        final KTable<String, Long> counts = source
               .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy((key, value) -> value)
                .count(Materialized.as("count"));

        counts.toStream().to(
            "streams-wordcount-output", 
            Produced.with(Serdes.String(), Serdes.Long())
        );

        return builder.build();
    }
}
```
(Note that for clarity, all import statements are omitted in code snippets. At the end of this section, we will show the complete code.)

As you can see, the `TopologyProvider` interface enforces you to provide a topology version. 
The version is required and you can't return `null`. Usually, you will return the version of your project (e.g : 1.0, 1.2-SNAPSHOT, etc).
Azkarra will use this version to generate a meaningful config `application.id` for your streams instance if no one is provided at runtime.

With Azkarra, you doesn't have to create a new `KafkaStreams` instance to run the `Topology`. This is done internally by the Azkarra API.

Instead of that, you are going to create a new `StreamsExecutionEnvironment` instance in the main method. 
The `StreamsExecutionEnvironment` is used to handle the lifecycle of one or multiple `KafkaStreams` instances. Furthermore, this can be used for
setting common configuration and/or registering multiple listeners to be applied on all running Kafka Streams instances. 

```java
StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create();
```
The code above, will create a new environment with an empty configuration.

Moreover, an Azkarra application is not limited to a single environment. 
For example, if you need to run a Kafka Streams application multiple time, you can create one environment for each target Kafka Cluster.
Thus, each environment will have its own configuration which can be passed through the method `DefaultStreamsExecutionEnvironment#create`.

Next, let's define the configuration that we are going to use for deploying the application.

Add the following lines of code in the `main()` method : 

```java
// Then, define the Kafka Streams configuration.
Map<String, Object> props = new HashMap<>();
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
```

Then, we can register the `WordCountTopologyProvider` previously define to our environment.

```java
Conf config = Conf.with("streams", props);
env.addTopology(WordCountTopologyProvider::new, Executed.as("wordcount").withConfig(config));
```

The streams configuration is passed through an `Executed` object. This object can also be used for naming or describing your topology.

Note that with Azkarra, we don't manipulate`Map` or `Properties` objects to configure a Kafka Streams 
application but we use a `Conf` object.

Finally, we are going to start the environment. Note, that the `start()` method is always non-blocking.

```java
env.start();
```

The complete code so far is this:

```java
package azkarra;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
public class SimpleStreamsApp {

    public static void main(final String[] args) {
        // First, create an environment for executing our Topology.
        StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create();

        // Then, define the Kafka Streams configuration,
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // And register the TopologyProvider to the environment.
        Conf config = Conf.with("streams", props);
        env.addTopology(
            WordCountTopologyProvider::new,
            Executed.as("wordcount").withConfig(config)
        );

        // Finally, start the streams environment.
        env.start();
    }
    public static class WordCountTopologyProvider implements TopologyProvider {
        @Override
        public String version() { return "1.0"; }

        @Override
        public Topology get() {
            final StreamsBuilder builder = new StreamsBuilder();

            final KStream<String, String> source = builder.stream("streams-plaintext-input");

            final KTable<String, Long> counts = source
                    .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                    .groupBy((key, value) -> value)
                    .count(Materialized.as("count"));

            counts.toStream().to(
                    "streams-wordcount-output",
                    Produced.with(Serdes.String(), Serdes.Long())
            );

            return builder.build();
        }
    }
}
```

Before building and running the application, let's make our `TopologyProvider` a little more configurable.

## Make a TopologyProvider Configurable 

Currently, the names of the source and sink topics are hard-coded. This is also the case for the materialized state store `count`.
To make our topology configurable, we will modify the `WordCountTopologyProvider` class to implement the interface `io.streamthoughts.azkarra.api.config.Configurable`.

This interface defines a method `configure` that accepts an argument of type `Conf`.

```java
public static class WordCountTopologyProvider implements TopologyProvider, Configurable {

    private String topicSource;
    private String topicSink;
    private String stateStoreName;

    @Override
    public void configure(final Conf conf) {
        topicSource = conf.getString("topic.source");
        topicSink = conf.getString("topic.sink");
        stateStoreName = conf.getOptionalString("state.store.name").orElse("count");
    }
    @Override
    public String version() { return "1.0"; }

    @Override
    public Topology get() {
        final StreamsBuilder builder = new StreamsBuilder();

        final KStream<String, String> source = builder.stream(topicSource);

        final KTable<String, Long> counts = source
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy((key, value) -> value)
                .count(Materialized.as(stateStoreName));

        // need to override value serde to Long type
        counts.toStream().to(topicSink, Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }
}
```

Now, we can update the configuration defined above in order to configure our `WordCountTopologyProvider` class.
The `StreamsExecutionEnvironment` will be responsible for invoking the method `configure()` before retrieving the `Topology`.

For doing this, we can use the helper class `ConfBuilder`.

```java
Conf config = ConfBuilder.newConf()
    .with("streams", props)
    .with("topic.source", "streams-plaintext-input")
    .with("topic.sink", "streams-wordcount-output")
    .with("state.store.name", "WordCount")
    .build();

    // And register the TopologyProvider to the environment.
    env.addTopology(
        WordCountTopologyProvider::new,
        Executed.as("wordcount").withConfig(config)
    );
```

Instead of configuring our streams and our provider on the topology-level, we can passed the configuration to the `StreamsExecutionEnvironment`. 
All topologies will then automatically inherit the configuration of their `StreamsExecutionEnvironment`.

```java
StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create(config);
```

Making a topology configurable is always recommend. This can be useful, for example, if you need to test a topology with different parameters.

The complete code so far is this:

```java
package azkarra;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.ConfBuilder;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SimpleStreamsApp {

    public static void main(final String[] args) {
        // First, define the Kafka Streams configuration,
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final Conf config = ConfBuilder.newConf()
            .with("streams", props)
            .with("topic.source", "streams-plaintext-input")
            .with("topic.sink", "streams-wordcount-output")
            .with("state.store.name", "WordCount")
            .build();

        // Then, create an environment for executing our Topology.
        StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create(config);

        // And register the TopologyProvider to the environment.
        env.addTopology(WordCountTopologyProvider::new, Executed.as("wordcount"));

        // Finally, start the streams environment.
        env.start();
    }

    public static class WordCountTopologyProvider implements TopologyProvider , Configurable {

        private String topicSource;
        private String topicSink;
        private String stateStoreName;

        @Override
        public void configure(final Conf conf) {
            topicSource = conf.getString("topic.source");
            topicSink = conf.getString("topic.sink");
            stateStoreName = conf.getOptionalString("state.store.name").orElse("count");
        }

        @Override
        public String version() { return "1.0"; }

        @Override
        public Topology get() {
            final StreamsBuilder builder = new StreamsBuilder();

            final KStream<String, String> source = builder.stream(topicSource);

            final KTable<String, Long> counts = source
                    .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                    .groupBy((key, value) -> value)
                    .count(Materialized.as(stateStoreName));

            counts.toStream().to(topicSink, Produced.with(Serdes.String(), Serdes.Long()));

            return builder.build();
        }
    }
}
```
There is still one missing piece to our application. 
Indeed, it's always recommend to configure a Java shutdown hook to ensure that our JVM shutdowns are handled gracefully.

## Adding a ShutdownHook
For adding a shutdown hook we are going to refactor our application by introducing a new concept called `AzkarraContext`.
An `AzkarraContext` is used to manage lifecycle of one or more `StreamsExecutionEnvironment`. Like an environment an `AzkarraContext`
can also be configured and all environments will automatically inherit from it. 

Replace your `main()` method with the code below : 

```java
public static void main(final String[] args) {
    // First, define the Kafka Streams configuration,
    Map<String, Object> props = new HashMap<>();
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

    final Conf config = ConfBuilder.newConf()
        .with("streams", props)
        .with("topic.source", "streams-plaintext-input")
        .with("topic.sink", "streams-wordcount-output")
        .with("state.store.name", "WordCount")
        .build();

    // Then, create an environment for executing our Topology.
    StreamsExecutionEnvironment env = DefaultStreamsExecutionEnvironment.create();

    // And register the TopologyProvider to the environment.
    env.addTopology(WordCountTopologyProvider::new, Executed.as("wordcount"));

    // Finally, create and start an Azkarra Context.
    DefaultAzkarraContext.create(config)
        .addExecutionEnvironment(env)
        .setRegisterShutdownHook(true)
        .start();
}
```
As you can see, this is the `AzkarraContext` which is responsible for starting the environment. 
In addition, we have moved the configuration from the `StreamsExecutionEnvironment` to the `AzkarraContext`.

The Azkarra API is designed to allow sensible overriding of properties. Configuration are considered in the following order :

- Topology (using the `Executed`).
- StreamsExecutionEnvironment (using `create()` or `setConfiguration`).
- AzkarraContext (using `create()` or `setConfiguration`).

(Note : You should always have a single `AzkarraContext` per application even if Azkarra does not enforce a singleton pattern.)

Now, we are ready to build and run our application.

## Running You App on Docker
Before running this example, you will have to start a Kafka Cluster. For that purpose we will 
use the official Kafka Docker images maintain by Confluent.Inc.

You can start a single-node Kafka Cluster using the file`docker-compose.yml`.

```
$> cd azkarra-getting-started
$> docker-compose up -d
```

Next, we have to create the two source and sink topics used by the topology.
For that, you can run the provided script : 

```bash
$> chmod u+x ./quickstart-create-wordcount-topics.sh
$> ./quickstart-create-wordcount-topics.sh
```

Or directly run those commands :

```bash
$> docker exec -it azkarra-cp-broker /usr/bin/kafka-topics \
--create --topic streams-plaintext-input --replication-factor 1 --partitions 3 --zookeeper zookeeper:2181

$> docker exec -it azkarra-cp-broker /usr/bin/kafka-topics \
--create --topic streams-wordcount-output --replication-factor 1 --partitions 3 --zookeeper zookeeper:2181
```

As a last step, we will package and run the Maven project :

```bash
$> mvn clean package && java -jar target/azkarra-quickstart-java-0.5.0.jar
```

Let's produce some input messages to Kafka topic `streams-plaintext-input` : 
```bash 
$> docker exec -it azkarra-cp-broker /usr/bin/kafka-console-producer \ 
--topic streams-plaintext-input --broker-list kafka:9092

Azkarra Streams
WordCount
I Heart Logs   
Kafka Streams
Making Sense of Stream Processing
```

Then consume from output topic `streams-wordcount-output` : 
```bash
$> docker exec -it azkarra-cp-broker /usr/bin/kafka-console-consumer --from-beginning \
--property print.key=true --property key.separator="-" \ 
--topic streams-wordcount-output --bootstrap-server kafka:9092 
```

Congratulation! You just run you first KafkaStreams application using Azkarra.

**But, Azkarra is not limited to that and can do much more.**  So let’s go ahead and explore what is making Azkarra so cool!

(In the following section we will omitted the code for the WordCountTopology for clarity)

## Simplifying our Application
In the previous example, we have manually created a `StreamsExecutionEnvironment` and added the `WordCountTopologyProvider`.
Actually, our objective was to give you an overview of Azkarra's main concepts. 
Defining environments in a programmatic way gives you flexibility but can be sometime cumbersome.

Our application can be simplified by using a default environment that will be created by the `ArkarraContext`.
For this, we are going to remove the environment instance previously created for directly registering the `WordCountTopologyProvider` 
to the context using the method `addTopology()`.

```java
AzkarraContext context = DefaultAzkarraContext.create(config);
context.addTopology(WordCountTopologyProvider.class, Executed.as("wordcount"));

context.setRegisterShutdownHook(true).start();
```

## Externalizing Context Configuration
The externalization of configuration is a requirement to deploy any application in production.

For that purpose, Azkarra provide the class so-called `AzkarraConf`. 
Internally, it uses the [Config](https://github.com/lightbend/config) library developed by Lightbend to ease the configuration of your application from an external file.

First, we will update the file `src/main/resources/application.conf` to contain the following code :

```
streams {
  bootstrap.servers = "localhost:9092"
  default.key.serde = "org.apache.kafka.common.serialization.Serdes$StringSerde"
  default.value.serde = "org.apache.kafka.common.serialization.Serdes$StringSerde"
}

topic {
  source = "streams-plaintext-input"
  sink = "streams-wordcount-output"
  store.name = "WordCount"
}
```

Next, we will replace our configuration by loading the `application.conf` using `AzkarraConf.create("application")`

The complete code so far is this:

```java
package azkarra;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import io.streamthoughts.azkarra.streams.config.AzkarraConf;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;

public class SimpleStreamsApp {

    public static void main(final String[] args) {
        // Create a new AzkarraConf from the classpath file application.conf
        AzkarraConf config = AzkarraConf.create();

        // Create and start a default AzkarraContext
        AzkarraContext context = DefaultAzkarraContext.create(config);
        context.addTopology(WordCountTopologyProvider.class, Executed.as("wordcount"));
        
        context.setRegisterShutdownHook(true).start();
    }

    public static class WordCountTopologyProvider implements TopologyProvider , Configurable {
    // code omitted for clarity....
    }
}
```

## Auto-Configure

Azkarra supports a mechanism of auto-configuration for automatically declaring environments and topologies to be run.
For that, we will introduce a new concept so-called `AzkarraApplication`.

Most of the time, the `AzkarraApplication` will be your main entry point when creating a new streams application.
Basically, this class is responsible for initializing and managing your application based on the specified configuration. 
Internally, it will create an `AzkarraContext` instance and the `StreamsExecutionEnvrionment` instances declared in your configuration.

```java
package azkarra;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.config.AzkarraConf;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;

public class SimpleStreamsApp {

    public static void main(final String[] args) {
        // Create configuration from externalized file.
        AzkarraConf config = AzkarraConf.create();

        new AzkarraApplication(SimpleStreamsApp.class)
            .setConfiguration(config)
            .setRegisterShutdownHook(true)
            .run(args);
    }

    public static class WordCountTopologyProvider implements TopologyProvider , Configurable {
    // code omitted for clarity....
    }
}
```

Next, update the `application.conf` to contain the following code :

Note that we have to create a parent config named `azkarra`.

```
azkarra {
  // The context configuration.
  context {
    streams {
      bootstrap.servers = "localhost:9092"
      default.key.serde = "org.apache.kafka.common.serialization.Serdes$StringSerde"
      default.value.serde = "org.apache.kafka.common.serialization.Serdes$StringSerde"
    }
  }
    
  // List of components to be registered into the AzkarraContext (e.g : TopologyProviders).
  components = [
    "azkarra.SimpleStreamsApp$WordCountTopologyProvider"
  ]
    
  // List of environments.
  environments = [
    {
      name = "__default"
      config = {}
      // List of streams jobs to execute
      jobs = [
        {
          name = "word-count-demo"
          description = "Kafka Streams WordCount Demo"
          topology = WordCountTopology
          // The topology configuration.
          config {
            topic {
              source = "streams-plaintext-input"
              sink = "streams-wordcount-output"
              store.name = "WordCount"
            }
          }  
        }
      ]
    }
  ]
}
```

## Embedded Http Server
Azkarra ships with an embedded non-blocking HTTP server (based on Undertow) to expose RESTful APIs. 
These APIs can be used not only to manage your local Kafka Streams instances and Topologies 
but also to execute interactive queries on your application states stores.

The HTTP-Server can be enable and configured via the `AzkarraApplication` as follows : 

```java
AzkarraConf config = AzkarraConf.create();

new AzkarraApplication(SimpleStreamsApp.class)
    .setConfiguration(config)
     // Enable http server and set the context.
    .enableHttpServer(true, HttpServerConf.with("localhost", 8080))
    .setRegisterShutdownHook(true)
    .run(args);
```

Let's have a look to some REST APIs.

* List local streams instances : 

```bash
$> curl -sX GET 'http://localhost:8080/api/v1/streams' | jq
```

* Get details about a running streams instance

```bash
$> curl -sX GET 'http://localhost:8080/api/v1/streams/word-count-demo-1-0' | jq
```

* Get metrics of a running streams instance

```bash
$> curl -sX GET 'http://localhost:8080/api/v1/streams/word-count-demo-1-0/metrics' | jq
```

* Query state store

```bash
$> curl -sX POST 'http://localhost:8080/api/v1/applications/word-count-demo-1-0/stores/count' --data '{ "type":"key_value", "query" : {"all":{} } }' | jq
```

## Annotations and Component Scan

For even more simplicity, Azkarra provides mechanisms for auto-discovering `TopologyProvider` classes.

Finally, the complete version of our application looks like this : 

```java
package azkarra;

import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.AzkarraStreamsApplication;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;

@AzkarraStreamsApplication
public class SimpleStreamsApp {

    public static void main(final String[] args) {
        AzkarraApplication.run(SimpleStreamsApp.class, args);
    }

    @Component
    public static class WordCountTopologyProvider implements TopologyProvider , Configurable {

        private String topicSource;
        private String topicSink;
        private String stateStoreName;

        @Override
        public void configure(final Conf conf) {
            topicSource = conf.getString("topic.source");
            topicSink = conf.getString("topic.sink");
            stateStoreName = conf.getOptionalString("state.store.name").orElse("count");
        }

        @Override
        public String version() {
            return "0.1";
        }

        @Override
        public Topology get() {
            final StreamsBuilder builder = new StreamsBuilder();

            final KStream<String, String> source = builder.stream(topicSource);

            final KTable<String, Long> counts = source
                    .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                    .groupBy((key, value) -> value)
                    .count(Materialized.as(stateStoreName));

            counts.toStream().to(topicSink, Produced.with(Serdes.String(), Serdes.Long()));

            return builder.build();
        }
    }
}
```

The `@AzkarraStreamsApplication` is a convenience annotation that adds all of the following:

 * `@EnableAutoConfig`: Tells Azkarra to auto-configure internal `AzkarraContext`
 * `@EnableAutoStart`: Tells Azkarra to automatically start any registered `TopologyProvider` using the default environment.
 * `@EnableEmbeddedHttpServer`: Tells Azkarra to start the embedded http-server.
 * `@ComponentScan`: Tells Azkarra to look for classes annotated with `@Component` in the current package.

We can simplify the `application.conf` by removing the environments sections. 
Azkarra will automatically load all your `TopologyProvider` by scanning your project classpath. 

Now, the `application.conf` should only contain the following code :

```
azkarra {
  // The context configuration.
  context {
    streams {
      bootstrap.servers = "localhost:9092"
      default.key.serde = "org.apache.kafka.common.serialization.Serdes$StringSerde"
      default.value.serde = "org.apache.kafka.common.serialization.Serdes$StringSerde"
    }
    topic {
      source = "streams-plaintext-input"
      sink = "streams-wordcount-output"
      store.name = "WordCount"    
    }
  }
}
```

## Azkarra Web UI

And, finally, we saved the best for last! 
Azkarra also provide a default WebUI for exploring your local streams application.
[Azkarra Streams Web UI](http://localhost:8080/ui)

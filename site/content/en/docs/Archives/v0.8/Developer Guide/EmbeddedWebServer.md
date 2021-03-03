---
date: 2020-09-25
title: "Embedded Web Server"
linkTitle: "Embedded Web Server"
weight: 85
description: >
  Configuring Azkarra embedded web server

---

## 1 Introduction

Azkarra packs with an embedded web server that exposes several REST endpoints allowing you to deploy, manage and monitor your Kafka Streams topologies. In addition, the embedded web server is also used to execute interactive queries.

Internally Azkarra relies on [Undertow](http://undertow.io/) a high performance non-blocking web-server.

## 2 Enabling Web Server

The embedded web server will be automatically enabled if your main Java class is annotated with either `@AzkarraStreamsApplication` or `@EnableEmbeddedHttpServer`.

**Example** :

```java
@AzkarraStreamsApplication
public class SimpleStreamsApp {

    public static void main(final String[] args) {
        AzkarraApplication.run(SimpleStreamsApp.class, args);
    }
}
```

In addition, the embedded web server can be enable using the `AzkarraApplication#enableHttpServer()` method.

```java
var serverConfig = ServerConfig
    .newBuilder()
    .setListener("localhost")
    .setPort(8080)
    .build();

application.enableHttpServer(true, serverConfig)
```

{{% alert title="StreamsConfig.APPLICATION_SERVER_CONFIG" color="info" %}}
You should note that when the web service is enable, Azkarra will automatically configure the `application.server` property for each running `KafkaStreams` instance.
{{% /alert %}}

## 3 Configuration

**Example** :

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

## 3 JSON Serialization

Azkarra allows you to run interactive queries to retrieve data from the state stores of Kafka Streams instances.
Records are returned in JSON format, using the [Jackson](https://github.com/FasterXML/jackson) serialization library.

### 3.1 Registering custom Jackson `Module`

Azkarra defines several JSON serializers that are provided by the module `azkarra-json-serializers` on which the Azkarra server depends. 

```xml
<dependency>
	<groupId>io.streamthoughts</groupId>
	<artifactId>azkarra-json-serializers</artifactId>
	<version>${azkarra.version}</version>
</dependency>
``` 

While these serializers are sufficient in most of the cases, you may have the need to provide your own custom 
serializers in order to use the interactive queries.

Therefore, Azkarra will automatically register all components that implements the `Module` interface into the internal `ObjectMapper` instance used for JSON serializaton. This allows you to register custom record serializers for interactive queries.

**Example using `@Component`**:

```java
@Factory
public class JacksonModuleFactory {

	@Component
	public Module customModule() {
		var module = new SimpleModule();
		module.addSerializer(MyCustomType.class, new MyCustomSerializer());
		return module;
	}
}
```

**Example using `AzkarraContext#register()`**:

```java
 context.registerComponent(Module.class, () -> {
        SimpleModule module = new SimpleModule();
        module.addSerializer(MyCustomType.class, new MyCustomSerializer());
        return module;
});
```

## 4 REST Extensions

Azkarra already exposes several REST endpoints that you can use to monitor and manage lifecycle of Kafka Streams instances.
But sometimes, you may want to expose additional REST endpoints to extend Azkarra capabilities.

Azkarra provides a mechanism, called REST extensions, that allows you to register JAX-RS resources that will be loaded and initialized by the Undertow server.

For adding REST extensions, you will first have to set the `server.rest.extensions` property to `true`. Then, you will have to implement the `AzkarraRestExtension` interface.

{{% alert title="AzkarraRestExtension" color="info" %}}
The `AzkarraRestExtension` interface extends the `Configurable` interface to give you access to the `azkarra.server` config.
{{% /alert %}}

**The `AzkarraRestExtension`** :

```java
/**
 * A pluggable interface to allow registration of new JAX-RS resources like REST endpoints.
 * The implementations are discovered using the standard Java {@link java.util.ServiceLoader} mechanism.
 *
 * Hence, the fully qualified name of the extension classes that implement the {@link AzkarraRestExtension}
 * interface must be add to a {@code META-INF/services/io.streamthoughts.azkarra.api.server.AzkarraRestExtension} file.
 */
public interface AzkarraRestExtension extends Configurable, Closeable {

    /**
     * Configures this instance with the specified {@link Conf}.
     * The configuration passed to the method correspond used for configuring the {@link EmbeddedHttpServer}
     *
     * @param configuration  the {@link Conf} instance used to configure this instance.
     */
    @Override
    default void configure(final Conf configuration) {

    }

    /**
     * The {@link AzkarraRestExtension} implementations should use this method to register JAX-RS resources.
     * @param restContext   the {@link AzkarraRestExtensionContext} instance.
     */
    void register(final AzkarraRestExtensionContext restContext);
}
```

The `register()` method accepts an `AzkarraRestExtensionContext` instance as the only parameter that gives you access to the `AzkarraContext` instance and the `javax.ws.rs.core.Configurable` that you should use to register one or more JAX-RS resources.

**The `AzkarraRestExtensionContext` interface**:  

```java
/**
 * This interfaces provides the capability for {@link AzkarraRestExtension} implementations
 * to register JAX-RS resources using the provided {@link Configurable} and to get access to
 * the {@link AzkarraContext} instance.
 *
 * @see AzkarraRestExtension
 */
public interface AzkarraRestExtensionContext {

    /**
     * Provides an implementation of {@link javax.ws.rs.core.Configurable} that
     * must be used to register JAX-RS resources.
     *
     * @return the JAX-RS {@link javax.ws.rs.core.Configurable}.
     */
    Configurable<? extends Configurable> configurable();

    /**
     * Provides the {@link AzkarraContext} instance that can be used to retrieve registered components.
     *
     * @return  the {@link AzkarraContext} instance.
     */
    AzkarraContext context();
}
```

**Example**:

```java
public class StopKafkaRestExtensionContext implements AzkarraRestExtension {

    @Override
    public void register(final AzkarraRestExtensionContext restContext) {
        AzkarraStreamsService service = restContext.context().getComponent(AzkarraStreamsService.class);
        restContext.configurable().register(new StopKafkaStreamsEndpoint(service));
    }
    
    @Path("/")
    public static class StopKafkaStreamsEndpoint {

        private final AzkarraStreamsService service;

        StopKafkaStreamsEndpoint(final AzkarraStreamsService service) {
            this.service = service;
        }

        @GET
        @Path("/stop")
        public void stop() {
            service.getAllStreams().forEach(applicationId -> service.stopStreams(applicationId, false));
        }
    }
}
```

Azkarra Server use the standard Java `java.util.ServiceLoader` mechanism to discover the REST implementations.

Therefore, for the REST extensions to be found, you will have to create a file `META-INF/services/io.streamthoughts.azkarra.api.server.AzkarraRestExtension` that contains the fully-qualified names of classes that implements the `StopKafkaRestExtensionContext` interface.

**Example**: 

```
com.example.StopKafkaRestExtensionContext
```

## 5 Server-Sent-Event (SSE)
     
 Server-Sent Events is a technology that allows a client to receive a stream of events from a server over one persistent HTTP connection. SSE is a lightweight alternative to Websocket that provides an efficient unidirectional communication protocol with standard mechanisms for handling errors.
 
 SSE can be a perfect solution :
 
 * When you need to stream state-changes (i.e: records) directly to a target system with low-latency.
 * When you want to use a unidirectional HTTP connection.
 * When data loss is acceptable.
 * When you don’t need or want to stream state-changes to an output Kafka topic.
 
 Here’s a few examples where SSE can be used : 
 * Updating real-time dashboard web application
 * Keeping a target system up-to-date with very fast changing data
 * Alerting
     
Azkarra provides the efficient and simple API called **Event Stream** to stream records 
from your local topology to HTTP-clients using SSE (since 0.8).
     
### 5.1 The Event Stream API

An `EventStream` object is used to capture and expose KeyValue records outside of a `KafkaStreams` topology.

Each `EventStream` is attached to a named type and a `BlockingRecordQueue` which is used to buffer 
records to be sent to HTTP-clients.

There is no limit to the number of `EventStream` you can create.

Here's how to build a new `EventStream` instance : 

```java
var eventStream = new EventStream.Builder("event-type-name")
    .withQueueSize(1000)
    .withQueueLimitHandler(LimitHandlers.dropHeadOnLimitReached())
    .build();
```

In addition to the `queue size`, you can also specify a `LimitHandler` that will be invoked each time a record cannot 
be offered to the internal queue because it's full. 

Azkarra provides three built-in `LimitHandler`s: 

* `LimitHandlers.dropHeadOnLimitReached()`: Retrieves and drops the head of the queue.
* `LimitHandlers.logAndContinueOnLimitReached()`: Logs the rejected record and continues.
* `LimitHandlers.throwExceptionOnLimitReached()`: Throws an exception when limit is reached. 

All `EventStream` objects must be provided through the `eventStreams` method of the `EventStreamProvider` interface
implemented by your `Topology` provider (i.e: `TopologyProvider`).

The `EventStreamProvider` interface : 

```java
public interface EventStreamProvider {
    List<EventStream> eventStreams();
}
```

In general, you will directly extend the `EventStreamSupport` class that exposes some convenient methods.
     
Here’s a complete topology example :
     
```java
@Component
// (O) Extend EventStreamSupport or implement the EventStreamProvider interface
public class ServerSentEventsWordCountTopology 
        extends EventStreamSupport 
        implements TopologyProvider {

    private EventStream<String, Long> wordCountStream;

    public ServerSentEventsWordCountTopology() {
        // (1) Create a new EventStream for word counts updates.
        wordCountStream = new EventStream.Builder("word-count")
            .withQueueSize(10_000)
            .withQueueLimitHandler(LimitHandlers.dropHeadOnLimitReached())
            .build();
        // (2) Register the EventStream
        addEventStream(wordCountStream);
    }

    @Override
    public Topology topology() {
        var builder = new StreamsBuilder();
        builder.<String, String>stream("text-lines")
            .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
            .groupBy((key, value) -> value)
            .count(Materialized.as("WordCount"))
            .toStream()
            // (3) send record to the EventStream      
            .foreach( (k, v) -> wordCountStream.send(k, v));  

        return builder.build();
    }

    @Override
    public String version() { return Version.getVersion(); }
}
```
     
### 5.2 The REST Endpoint

To subscribe to an event-streams and start receiving records from a specific stream application you can 
initialize an SSE connection thought the following REST endpoint : 

```
GET /api/v1/streams/(string: applicationId)/subscribe/(string: eventType)
```
     
### 5.2 How to deal with multiple instances of Kafka Streams ?

When subscribing to an event-stream only records from the local Kafka Streams instance are sent over the HTTP-Connection.
Thus you will have to establish one SSE connections for each Kafka Streams application.
     
### 5.3 Multiple HTTP-clients subscribing to the same event stream

One or more HTTP clients can subscribe to the same event stream. All clients will receive all records sent in the event stream.


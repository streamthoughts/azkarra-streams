---
date: 2020-05-11
title: "Embedded Web Server"
linkTitle: "Embedded Web Server"
weight: 85
description: >
  Configuring Azkarra embedded web server

---


## 1 Introduction

Azkarra packs with an embedded web server that exposes several REST endpoints allowing you to deploy, manage and monitor your Kafka Streams topologies. In addition, the embedded web server is also used to execute interactive queries.

Internally Azkarras relies on [Undertow](http://undertow.io/) a high performance non-blocking webserver.

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
application.enableHttpServer(true, HttpServerConf.with("localhost", 8080))
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
	<version>${azkarra.version}
</dependency>
``` 

While these serializers are sufficient in most of the cases, you may need to provide your own custom serializers in order to use the interactive queries.

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

Azkarra already exposes several REST endpoints that you can used to monitor and manage lifecycle of Kafka Streams instances.
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


---
date: 2020-02-12
title: "Error Management"
linkTitle: "Error Management"
weight: 8
description: >
  How to handle exception thrown during `KafkaStreams` execution ?
---

During the execution of `KafkaStreams` instance different types of errors may happens.

## 7.1 The `DeadLetterTopicExceptionHandler` class

Azkarra provides the `DeadLetterTopicExceptionHandler`, a `DeserializationExceptionHandler` allowing to send corrupted records into a dedicated topic. 

By default, the `DeadLetterTopicExceptionHandler` will send corrupted records to an sink topic named based on the source topic - i.e: `rejected-<source_topic_name>`.

In addition, the `DeadLetterTopicExceptionHandler` will enrich corrupted records with Kafka headers to help investigate the cause of the exception.

| Header                            | Type   | Description                                                  |
|---------------------------------- |--------|--------------------------------------------------------------|
|  `errors.exception.stacktrace`    | string | The exception stacktrace.                                    |     
|  `errors.exception.message`       | string | The exception message.                                       |
|  `errors.timestamp`               | string | The current epoch time in millisecond when exception ocurred.|       
|  `errors.exception.class.name`    | string | The exception class name.                                    |
|  `errors.record.topic`            | string | The source topic of the corrupted message.                   |
|  `errors.record.partition`        | string | The source partition of the corrupted message.               |
|  `errors.record.offset`           | string | The source offset of the corrupted message.                  |

### 7.1.1 Configuring `DeadLetterTopicExceptionHandler`

| Property                              | Type       | Description                                                                    |
|---------------------------------------|------------|--------------------------------------------------------------------------------|
|  `exception.handler.dead.letter.topic`        | String     | The name of the dead letter topic to be used to write rejected records.|     
|  `exception.handler.dead.letter.fatal.errors` | List       |List of exception classes on which the handler must fail.               |

### 7.1.2 Configuring `KafkaProducer`

By default, the `DeadLetterTopicExceptionHandler` uses the `KafkaProducer` attached to the internal `StreamThread`.
A dedicated `KafkaProducer` can be created by configuring handler producer using the property prefix `exception.handler.dead.letter.`.

### 7.1.3 Adding custom headers

You can configure additional header to be added to corrupted message using the prefix `exception.handler.dead.letter.headers.`.


## 7.2 The `SafeDeserializer` class

Azkarra provides a `SafeDeserializer` that can be used to wrap an existing `Deserializer` and catch any exception thrown during deserialization for returning a record called a *sentinel-object* that you filter later in the `Topology` (e.g null, "N/A", -1, etc).

### 7.2.1 Creating a `SafeDeserializer`

```java
SafeDeserializer deserializer = new SafeDeserializer<>(
    new GenericAvroSerde().deserializer(), // the delegating deserializer
    (GenericRecord)null     			   // the sentinel-object to return when an exception is catch
);
```

### 7.2.2 Configuring a `SafeDeserializer`

The sentinel-object to return can also be configured.

```java
SafeDeserializer<Double> deserializer = new SafeDeserializer<>(
    Serdes.Double().deserializer(), // the delegating deserializer
    Double.class    		        // the value type
);

Map<String, Object> configs = new HashMap<>();
configs.put(SafeDeserializerConfig.SAFE_DESERIALIZER_DEFAULT_VALUE_CONFIG, 0.0);
deserializer.configure(configs, false);
```

### 7.2.3 The `SafeSerde` class

The `SafeSerde` is an utility class allowing you to wrap existing `Serde` or `Deserializer`.

Behing the scene, `SafeSerde` uses the `SafeDeserializer` for wrapping existing `Deserializer`.

```java
Serde<String> stringSerde = SafeSerdes.Double();  
```

or 

```java
SafeSerdes.serdeFrom(Serdes.String(), 0.0);
```


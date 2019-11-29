---
date: 2019-11-29
layout: default
title: "Streams"
parent: "REST API Reference"
nav_order: 2
description: >
  Endpoints for retrieving information about streams instances running locally.
---

Endpoints for retrieving information about streams instances running locally.

Base URL: `/api/v1/streams`

## GET /api/v1/streams

Get a list of streams instances currently active on the local JVM.

**Example Request**
```
GET /api/v1/streams
Host: localhost:8080
```
**Example Response**
```json
[
  "word-count-topology-1-0"
]
```

## GET /api/v1/streams/(string: applicationId)

Get information about the local streams instance.

**Example Request**
```
GET /api/v1/streams/word-count-topology-1-0
Host: localhost:8080
```

**Example Response**
```json
{
  "since": "2019-10-04T14:06:06.137+02:00[Europe/Paris]",
  "state": {
    "since": "2019-10-04T14:06:06.461+02:00[Europe/Paris]",
    "state": "RUNNING"
  },
  "version": "1.0",
  "description": null,
  "config": {
    "bootstrap.servers": "localhost:9092",
    "application.server": "localhost:8080",
    "log.maxFileSize": 104857600,
    "default.value.serde": "org.apache.kafka.common.serialization.Serdes$StringSerde",
    "rocksdb.config.setter": "io.streamthoughts.azkarra.api.streams.rocksdb.DefaultRocksDBConfigSetter",
    "state.dir": "/tmp/kafka-streams/",
    "stats.enable": false,
    "default.key.serde": "org.apache.kafka.common.serialization.Serdes$StringSerde",
    "log.dir": "/var/log/kafka-streams/rocksdb",
    "stats.dumpPeriodSec": 30,
    "application.id": "word-count-topology-1-0"
  },
  "name": "WordCountTopology"
}
```

## GET /api/v1/streams/(string: applicationId)/status

Get current status about the running tasks for the streams application.

**Example Request**
```
GET /api/v1/streams/word-count-topology-1-0/tasks
Host: localhost:8080
```

**Example Response**
```json

```

## GET /api/v1/streams/(string: applicationId)/config

Get the configuration for the streams application.

**Example Request**
```
GET /api/v1/streams/word-count-topology-1-0/config
Host: localhost:8080
```

**Example Response**
```json
{
  "stats.dumpPeriodSec": 30,
  "default.value.serde": "org.apache.kafka.common.serialization.Serdes$StringSerde",
  "rocksdb.config.setter": "io.streamthoughts.azkarra.api.streams.rocksdb.DefaultRocksDBConfigSetter",
  "bootstrap.servers": "localhost:9092",
  "application.server": "localhost:8080",
  "state.dir": "/tmp/kafka-streams/",
  "stats.enable": false,
  "log.maxFileSize": 104857600,
  "default.key.serde": "org.apache.kafka.common.serialization.Serdes$StringSerde",
  "log.dir": "/var/log/kafka-streams/rocksdb",
  "application.id": "word-count-topology-1-0"
}
```

## GET /api/v1/streams/(string: applicationId)/metrics

Get current metrics for the streams application.

**Accepted query parameters :**

| Parameter/Value      | Description                                  |
|----------------------|----------------------------------------------|
|  `format=prometheus` | Get streams metrics for Prometheus scrapping |
|  `filter_empty`      | Filter all streams metrics with empty value  |

**Example Request**
```
GET /api/v1/streams/word-count-topology-1-0/metrics
Host: localhost:8080
```

**Example Response**
```json
{ 
  [{
    "name": "consumer-fetch-manager-metrics",
    "metrics": [
      {
        "name": "bytes-consumed-rate",
        "description": "The average number of bytes consumed per second for a topic",
        "tags": {
          "client-id": "word-count-topology-1-0-5f27b08e-f7a2-408e-b8d1-72ddf4d9adc6-StreamThread-1-consumer",
          "topic": "word-count-topology-1-0-count-repartition"
        },
        "value": 0
      },
      {
        "name": "bytes-consumed-total",
        "description": "The total number of bytes consumed",
        "tags": {
          "client-id": "word-count-topology-1-0-5f27b08e-f7a2-408e-b8d1-72ddf4d9adc6-StreamThread-1-consumer"
        },
        "value": 0
      },
      {
        "name": "fetch-latency-avg",
        "description": "The average time taken for a fetch request.",
        "tags": {
          "client-id": "word-count-topology-1-0-5f27b08e-f7a2-408e-b8d1-72ddf4d9adc6-StreamThread-1-consumer"
        },
        "value": 501.46875
      },
    ...
   }]
}
```

## GET /api/v1/streams/(string: applicationId)/metrics/group/{group}

Get current metrics for the streams application and metric group.

**Accepted query parameters :**

| Parameter/Value      | Description                                  |
|----------------------|----------------------------------------------|
|  `format=prometheus` | Get streams metrics for Prometheus scrapping |
|  `filter_empty`      | Filter all streams metrics with empty value  |

**Example Request**
```
GET /api/v1/streams/word-count-topology-1-0/metrics/metrics/group/app-info
Host: localhost:8080
```

## GET /api/v1/streams/(string: applicationId)/metrics/group/{group}/name/{name}

Get current metrics for the streams application, metric group and name.

**Accepted query parameters :**

| Parameter/Value      | Description                                  |
|----------------------|----------------------------------------------|
|  `format=prometheus` | Get streams metrics for Prometheus scrapping |
|  `filter_empty`      | Filter all streams metrics with empty value  |

**Example Request**
```
GET /api/v1/streams/word-count-topology-1-0/metrics/metrics/group/app-info/name/version
Host: localhost:8080
```

## GET /api/v1/streams/(string: applicationId)/metrics/group/{group}/name/{name}/value

Get current metrics for the streams application, metric group and name.

**Example Request**
```
GET /api/v1/streams/word-count-topology-1-0/metrics/metrics/group/app-info/name/version/value
Host: localhost:8080
```

## POST /api/v1/streams/(string: applicationId)/restart

Restart the local active streams instance.

**Example Request**
```
POST /api/v1/streams/word-count-topology-1-0/restart
Host: localhost:8080
```

**Example Response**
```
RESPONSE 200/OK
```

## DELETE /api/v1/streams/(string: applicationId)/stop

Stop the local active streams instance.

Request JSON Object:
 	
 * **cleanup** (boolean): Flag to indicate if the local streams states should be cleaned up.
 
**Example Request**
```
DELETE /api/v1/streams/word-count-topology-1-0/stop
Host: localhost:8080
{
  cleanup: false
}
```

**Example Response**
```
RESPONSE 200/OK
```

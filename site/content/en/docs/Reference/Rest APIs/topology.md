---
date: 2019-11-29
layout: default
title: "Topologies"
parent: "REST API Reference"
nav_order: 3
description: >
  Endpoints for retrieving information about the application health.
---

Endpoints for retrieving information about available topologies.

Base URL: `/api/v1/topologies`

##  GET /api/v1/topologies

Returns a list of Topologies available on the local application.

**Example Request**
```
GET /api/v1/topologies
Host: localhost:8080
```
**Example Response**
```json
[
  {
    "name": "io.streamthoughts.azkarra.example.topology.WordCountTopology",
    "version": "1.0",
    "description": "Kafka Streams WordCount Demo",
    "aliases": [
      "WordCount",
      "WordCountTopology"
    ]
  }
]
```

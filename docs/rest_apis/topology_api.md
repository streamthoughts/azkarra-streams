---
layout: default
title: "Topologies"
parent: "REST API Reference"
nav_order: 3
---

# REST API - Topologies
{: .no_toc }

Endpoints for retrieving information about available topologies.

Base URL: `/api/v1/topologies`

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

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
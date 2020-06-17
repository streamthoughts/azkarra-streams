---
date: 2019-11-29
layout: default
title: "Health"
parent: "REST API Reference"
nav_order: 7
description: >
  Endpoints for retrieving information about the application health.
---

Endpoints for retrieving information about the application health.

Base URL: `/health`

##  GET /health

Returns the information about the application.

**Example Request**

```
GET /health
Host: localhost:8080
```
**Example Response**

```json
{
  "status": "UP",
  "details": {
    "applications": {
      "name": "applications",
      "status": "UP",
      "details": {
        "basic-word-count-topology-0-3": {
          "status": "UP",
          "details": {
            "state": "RUNNING"
          }
        }
      }
    }
  }
}
```

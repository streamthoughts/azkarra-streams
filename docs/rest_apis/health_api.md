---
layout: default
title: "Info"
parent: "REST API Reference"
nav_order: 7
---

# REST API - Health
{: .no_toc }

Endpoints for retrieving information about the application health.

Base URL: `/health`

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

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
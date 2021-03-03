---
date: 2019-11-29
layout: default
title: "Version"
parent: "REST API Reference"
nav_order: 5
description: >
  Endpoints for retrieving information about Azkarra version.
---

Endpoints for retrieving information about Azkarra version.

Base URL: `/version`

##  GET /version

Returns the current version of Azkarra

**Example Request**
```
GET /version
Host: localhost:8080
```
**Example Response**
```json
{
  "azkarraVersion": "0.1"
}
```

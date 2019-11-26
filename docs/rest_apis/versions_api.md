---
layout: default
title: "Version"
parent: "REST API Reference"
nav_order: 5
---

# REST API - Version
{: .no_toc }


Endpoints for retrieving information about Azkarra version.

Base URL: `/version`

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

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
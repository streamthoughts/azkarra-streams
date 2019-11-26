---
layout: default
title: "Info"
parent: "REST API Reference"
nav_order: 6
---

# REST API - Version
{: .no_toc }

Endpoints for retrieving information about the application.

Base URL: `/info`

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

##  GET /info

Returns the information about the application.

**Example Request**
```
GET /info
Host: localhost:8080
```
**Example Response**
```json
{
  app: {
    java: {
      version: "11.0.1"
    },
    name: "azkarra-quickstart-java",
    description: "Simple Azkarra Streams project",
    encoding: "UTF-8",
    version: "{{ site.current_version }}"
  }
}
```
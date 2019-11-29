---
date: 2019-11-29
layout: default
title: Azkarra Streams REST Interface
has_children: true
nav_order: 4

description: >
  REST API Reference.

---

Azkarra Streams embeds an HTTP server which exposes a REST API to manage available topologies and streams jobs running locally.
By default, the server listens on port 8080.

## Content-Type

Currently, the REST API only supports `application/json` as both the request and response entity content type.
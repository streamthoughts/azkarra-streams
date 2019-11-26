---
layout: default
title: REST API Reference
has_children: true
nav_order: 4
---
# Azkarra Streams REST Interface

Azkarra Streams embeds an HTTP server which exposes a REST API to manage available topologies and streams jobs running locally.
By default, the server listens on port 8080.

## Content-Type

Currently, the REST API only supports `application/json` as both the request and response entity content type.
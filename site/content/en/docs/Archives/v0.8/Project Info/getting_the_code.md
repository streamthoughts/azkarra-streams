---
date: 2020-05-09
title: "Getting the code"
linkTitle: "Getting the code"
weight: 10
description: >
  How to get and build the code ?
---

## Prerequisites for building Azkarra:**

* Git
* Maven (we recommend version 3.5.3)
* Java 11

## Building Azkarra Streams

The code of Azkarra Streams is kept in GitHub. You can check it out like this:

```bash
$ git clone https://github.com/streamthoughts/azkarra-streams.git
```


The project uses Maven, you can build it like this:

```bash
$ cd azkarra-streams
$ mvn clean package -DskipTests
```

## Building Azkarra Website

The source code for Azkarra website is kept in directory `./site` of the project.

Azkarra uses the open-source static site generators [Hugo](https://gohugo.io/). Hugo Extended must be installed locally to build the website. In addition, Azkarra website is based on theme [Docsy](https://www.docsy.dev/).

To build and deploy the website locally, you can run the following commands:

```bash
$ git submodule update --remote
$ cd site 
$ git submodule sync && git submodule update --init --recursive
$ npm install
$ hugo server --watch --verbose --disableFastRender
```

The website is accessible at address : <a href="http://localhost:1313/azkarra-streams/">http://localhost:1313/azkarra-streams/</a>
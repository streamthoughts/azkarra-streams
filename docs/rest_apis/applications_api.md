---
layout: default
title: "Applications"
parent: "REST API Reference"
nav_order: 1
---

# REST API - Applications
{: .no_toc }


Endpoints for retrieving information about running streams applications and state stores.

Base URL: `/api/v1/applications`

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## GET /api/v1/applications

Get a list of streams instances currently active on the locally JVM.

**Example Request**
```
GET /api/v1/applications/
Host: localhost:8080
```
**Example Response**
```json
[
  "word-count-topology-1-0"
]
```

## GET /api/v1/applications/(string: applicationId)

Get information about the servers, including topic/partitions assignments and available states stores, for the streams application.

**Example Request**
```
GET /api/v1/applications/word-count-topology-1-0
Host: localhost:8080
```
**Example Response**
```json
[
  "word-count-topology-1-0"
]
```
## GET /api/v1/applications/(string: applicationId)/topology

Get the topology DAG for the streams application.

**Example Request**
```
GET /api/v1/applications/word-count-topology-1-0/topology
Host: localhost:8080
```
**Example Response**
```json

```

## POST /api/v1/applications/(string: applicationId)/stores/(string: name)

Execute an interactive queries  

**Request JSON Object:**
 	
 * **type** (string): The type of queried state store. Supported values are : [`key_value`, `session`, `window`, `timestamped_key_value`, `timestamped_window`].
 * **set_options** (map[string, String] : Options used to execute the query
    * `retries` : The maximum number of attempts to run after failed access to a given local state store.
    * `retry_backoff_ms` : The time to wait before attempting to retry a failed access to a given local state store.
    * `query_timeout_ms` : This limit the total time of state store execute.
    * `remote_access_allowed` : Is remote access is allowed for this execute.
 * **query**: (map[string, String]) : The query clause and parameters.
 
Currently, Azkarra supports the following queries :  

* **`all`** : Get all key-value pairs in the specified store
    * supported store types: [`key_value`].
    * (no parameter)
    
* **`count`** : Approximate the number of entries in the specified store
    * supported store types: [`key_value`].
    * (no parameter)
     
* **`fetch`** : Get the value corresponding to the specified key from the time window (in millisecond).
    * supported store types : [`window`].
    * parameters : 
        * `key`  
        * `time`  
          
* **`fetch_key_range`** : Get all the key-value pairs in the given key range and time range from all the existing windows.
    *  supported store types : [`window`].
    * parameters : 
        * `key_from`   
        * `key_to`   
        * `time_from`   
        * `time_to`   
            
* **`get`** : Get the value corresponding to the specified key.
    * supported store types : [`key_value`].
    * parameters : 
        * `key`

* **`range`** : Get all the key-value pairs in the given key range.
    * supported store types : [`key_value`].
    * parameters : 
        * `key_from`
        * `key_to`
 
                        
**Example Request**
```bash
POST /api/v1/applications/word-count-topology-1-0/stores/count
Host: localhost:8080
{ 
  "query" : { 
    "get": {
         "key": "streams"
    }
  },
  "type":"key_value",
  "set_options":{}
}
```
**Example Response**
```json
{
  "timeout": false,
  "server": "localhost:8080",
  "took": 10,
  "result": {
    "total": 1,
    "failure": [],
    "success": [
      {
        "remote": false,
        "total": 1,
        "records": [
          {
            "key": "streams",
            "value": 3
          }
        ],
        "server": "localhost:8080"
      }
    ]
  },
  "status": "SUCCESS"
}
```
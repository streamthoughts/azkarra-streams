/*
 * Copyright 2019 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
<template>
<div id="query-container" class="container-fluid">
<div class="row">
    <div class="col">
        <h3 class="mb-6">Query Results</h3>
        <span class="text-no-data" v-if="!response.status">No query executed</span>
        <div class="query-response-container" v-if="response.status">
           <ul class="list-inline left-border">
              <li class="list-inline-item">
                Status: <span class="badge badge-success badge-pill">{{ response.status }}</span>
              </li>
              <li class="list-inline-item">
                Took:  <span class="badge badge-info badge-pill">{{ response.took }}ms</span>
              </li>
              <li class="list-inline-item">
                Server: <span class="badge badge-secondary badge-pill">{{ response.server }}</span>
              </li>
           </ul>
            <div v-if="response.result.success">
                <template v-for="response in response.result.success">
                    <ul class="list-group query-server-response-group">
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            <span class="badge badge-success">server: {{ response.server }} </span>
                            <span class="badge badge-info badge-pill">{{ response.total }}</span>
                        </li>
                    <template v-for="record in response.records">
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            <span>key : <span class="badge badge-dark">{{ record.key }}</span> </span>
                            <span>value : <span class="badge badge-dark"> {{ record.value }}</span></span>
                    </template>
                    </ul>
                </template>
            </div>
            <div v-if="response.result.failure">
                <template v-for="response in response.result.failure">
                    <ul class="list-group query-server-response-group">
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            <span class="badge badge-danger">server: {{ response.server }} </span>
                            <span class="badge badge-primary badge-pill">{{ response.total }}</span>
                        </li>
                        <template v-for="error in response.errors">
                            <li class="list-group-item">message : {{ error.message }}</li>
                        </template>
                    </ul>
                </template>
            </div>
        </div>
    </div>
    <div class="col-4">
        <div class="bg-white box-shadow p-3">
            <h4 class="mb-3">Store</h4>
            <form>
                <div class="input-group mb-3">
                    <div class="input-group-prepend">
                        <label class="input-group-text" for="query-application-id">Application</label>
                    </div>
                    <select v-on:change="fetchApplicationMetadata"
                            v-model="query.application" class="custom-select" id="query-application-id">
                        <template v-for="app in applications">
                            <option v-bind:value="app">{{ app }}</option>
                        </template>
                    </select>
                </div>
                <div class="row">
                    <div class="col-6 mb-3">
                        <div class="input-group" v-if="query.application">
                            <div class="input-group-prepend">
                                <div class="input-group-text">Store</div>
                            </div>
                              <select v-model="query.store" class="custom-select" id="query-store-name">
                                  <template v-for="stateStoreName in stateStoreNames">
                                      <option v-bind:value="stateStoreName">{{ stateStoreName }}</option>
                                  </template>
                              </select>
                        </div>
                    </div>
                    <div class="col-6 mb-3">
                        <div class="input-group" v-if="query.store">
                            <div class="input-group-prepend">
                                <label class="input-group-text" for="query-store-type">Type</label>
                            </div>
                            <select v-model="query.type" class="custom-select" id="query-store-type">
                                <template v-for="store in stores">
                                    <option v-bind:value="store">{{ store.typeLabel }}</option>
                                </template>
                            </select>
                        </div>
                    </div>
                </div>
                <div class="input-group mb-3" v-if="query.type">
                    <div class="input-group-prepend">
                        <label class="input-group-text" for="query-store-operation">Operation</label>
                    </div>
                    <select v-model="query.operation" class="custom-select" id="query-store-operation">
                        <template v-if="query.type" v-for="operation in query.type.operations">
                            <option v-bind:value="operation">{{ operation.name }}</option>
                        </template>
                    </select>
                </div>
                <template v-if="query.operation && query.operation.params.length > 0">
                    <hr class="mb-4">
                    <h4 class="mb-3">Parameters</h4>
                    <span v-if="!query.operation.params.length">(no parameters)</span>
                    <div class="input-group mb-3">
                        <template v-if="query.operation" v-for="param in query.operation.params">
                         <div class="input-group mb-3">
                                <div class="input-group-prepend">
                                    <div class="input-group-text">{{ param }}</div>
                                </div>
                                <input v-model:value="query.params[param]"
                                    type="text"
                                    class="form-control"
                                    id="query-params-{{ param }}">
                         </div>
                        </template>
                    </div>
                </template>
                <hr class="mb-4">
                <h4 class="mb-3">Options</h4>
                <div class="row mb-3">
                    <label for="query-options-timeout" class="col-sm-2 col-form-label">Timeout</label>
                    <div class="col-sm-10">
                        <input v-model:value="query.options.query_timeout_ms"
                               type="text"
                               class="form-control"
                               id="query-options-timeout"
                               placeholder="in milliseconds">
                    </div>
                </div>
                <div class="row mb-3">
                    <label for="query-options-retries" class="col-sm-2 col-form-label">Retries</label>
                    <div class="col-sm-10">
                        <input v-model:value="query.options.retries"
                               type="text"
                               class="form-control"
                               id="query-options-retries">
                    </div>
                </div>
                <div class="row mb-3">
                    <label for="query-options-retry-backoff" class="col-sm-2 col-form-label">Retries Backoff</label>
                    <div class="col-sm-10">
                        <input v-model:value="query.options.retry_backoff_ms"
                               type="text"
                               class="form-control"
                               id="query-options-backoff_ms"
                               placeholder="in milliseconds">
                    </div>
                </div>
                <div class="row mb-3">
                    <div class="col-sm-2">Allow remote access</div>
                    <div class="col-sm-10">
                        <div class="form-check">
                            <input v-model="query.options.remote_access_allowed"
                                   class="form-check-input"
                                   type="checkbox"
                                   id="query-options-remote-access">
                            <label class="form-check-label" for="query-options-remote-access">(allowed)</label>
                        </div>
                    </div>
                </div>
                <button v-on:click="executeQuery()" class="btn btn-primary">Execute</button>
            </form>
        </div>
    </div>
</div>
</div>
</template>
<script>
import azkarra from '../services/azkarra-api.js'

const windowOperations = [
   { name: 'fetch', params:['key', 'time'] },
   { name: 'fetch_range', params:['key', 'fromTime', 'toTime']},
   { name: 'fetch_key_range', params:['keyFrom', 'keyTo', 'fromTime', 'toTime']},
   { name: 'fetch_all', params:['fromTime', 'toTime']},
   { name: 'all', params:[]},
];

const keyValueOperations = [
   { name: 'get', params:['key'] },
   { name: 'range', params:['keyFrom', 'keyTo']},
   { name: 'all', params:[]},
   { name: 'count', params:[]}
];

const sessionOperations = [
   { name: 'fetch', params:['key'] },
   { name: 'fetch_range', params:['keyFrom', 'keyTo']}
]

export default {
  data: function () {
    return {
      response: {},
      query: {
        params: {},
        options: {
            query_timeout_ms: 1000,
            retries: 100,
            retry_backoff_ms: 100,
            remote_access_allowed:true
        }
      },
      applications: [],
      applicationServers: [],
      stateStoreNames: [],
      stores: [
        { typeLabel : 'KeyValue', typeValue :'key_value', operations : keyValueOperations},
        { typeLabel : 'TimestampedKeyValue', typeValue :'timestamped_key_value', operations : keyValueOperations},
        { typeLabel : 'Window', typeValue :'window', operations : windowOperations },
        { typeLabel : 'TimestampedWindow', typeValue :'window', operations : windowOperations },
        { typeLabel : 'Session', typeValue :'session', operations : sessionOperations}
      ]
    }
  },
  created () {
     this.fetchLocalActiveStreams()
  },
  watch: {
    '$route': 'fetchLocalActiveStreams'
  },
  methods: {
    fetchLocalActiveStreams() {
      var that = this;
      azkarra.fetchLocalActiveStreamsList().then(function(data){
        that.applications = data
      })
    },

    fetchApplicationMetadata() {
      var that = this;
      azkarra.fetchApplicationMetadata({id: this.query.application }).then(function(data){
        let storeNames = new Set();
        data.forEach(server => {
          server.stores.forEach(s => storeNames.add(s));
          that.applicationServers.push(server.host + ":" + server.port);
        });
        storeNames.forEach(s => that.stateStoreNames.push(s));
      });
    },

    executeQuery() {
      var that = this;
      let query = {
        params : this.query.params,
        type: this.query.type.typeValue,
        operation: this.query.operation.name,
        store: this.query.store,
        application : this.query.application,
        options: this.query.options
      };
      azkarra.sendQueryStateStore(query).then(function(data){
        that.response = data
      });
    }
  }
}
</script>



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
    <div class="col-8">
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
                <li class="list-inline-item" v-if="response.error">
                    error: <span class="badge badge-secondary badge-pill">{{ response.error }}</span>
                </li>
            </ul>
            <div class="custom-control custom-checkbox custom-control-inline">
                <input v-model="displayJsonValue"
                    class="custom-control-input"
                    type="checkbox"
                    id="display-option-json-value">
                <label class="custom-control-label" for="display-option-json-value">JSON Values</label>
            </div>
            <div class="custom-control custom-checkbox custom-control-inline">
                <input v-model="showRecordIndex"
                    type="checkbox"
                    id="display-record-index"
                    class="custom-control-input">
                <label class="custom-control-label" for="display-record-index">Show Row Index</label>
            </div>
            <div class="custom-control custom-checkbox custom-control-inline">
                <input v-model="showRecordKey"
                    type="checkbox"
                    id="display-record-key"
                    class="custom-control-input">
                 <label class="custom-control-label" for="display-record-key">Show Record Keys</label>
            </div>
            <div v-if="isTimestampedStore()" class="custom-control custom-checkbox custom-control-inline">
                <input v-model="showRecordTimestamp"
                    type="checkbox"
                    id="display-record-timestamp"
                    class="custom-control-input">
                 <label class="custom-control-label" for="display-record-timestamp">Show Record Timestamps</label>
            </div>
            <div id="query-response-list">
                <nav>
                    <div class="nav nav-tabs" id="nav-tab" role="tablist">
                        <template v-if="response.result.success">
                            <template v-for="(success,  responseIndex) in successResultsOrderByServer">
                                <a class="nav-item nav-link"
                                    v-bind:class="{ active: responseIndex == 0 }"
                                    v-bind:id="'nav-'+ normalizeHtmlAttr(success.server) + '-tab'"
                                    v-bind:href="'#nav-' + normalizeHtmlAttr(success.server)"
                                    v-bind:aria-controls="'nav-' + normalizeHtmlAttr(success.server)"
                                    data-toggle="tab"
                                    v-bind:aria-selected="responseIndex == 0 ? 'true' : 'false'">
                                    <span class="badge badge-success">server: {{ success.server }} </span>
                                </a>
                            </template>
                        </template>
                        <template v-if="response.result.failure">
                            <template v-for="(failure, responseIndex)  in failureResultsOrderByServer">
                                <a class="nav-item nav-link"
                                    v-bind:class="{ active: responseIndex == 0 && !response.result.success }"
                                    v-bind:id="'nav-'+ normalizeHtmlAttr(failure.server) + '-tab'"
                                    v-bind:href="'#nav-' + normalizeHtmlAttr(failure.server)"
                                    v-bind:aria-controls="'nav-' + normalizeHtmlAttr(failure.server)"
                                    data-toggle="tab"
                                    v-bind:aria-selected="responseIndex == 0 ? 'true' : 'false'">
                                    <span class="badge badge-danger">server: {{ failure.server }} </span>
                                    <span class="badge badge-primary badge-pill">{{ failure.total }}</span>
                                </a>
                            </template>
                        </template>
                    </div>
                </nav>
                <div class="tab-content" id="nav-tabContent">
                    <template v-for="(success, responseIndex) in successResultsOrderByServer">
                        <div class="tab-pane fade"
                            v-bind:class="{ active: responseIndex == 0, show: responseIndex == 0 }"
                            v-bind:id="'nav-' + normalizeHtmlAttr(success.server)"
                             v-bind:aria-labelledby="'nav-' + normalizeHtmlAttr(success.server) + '-tab'"
                            role="tabpanel">
                            <div class="tab-pane-content bg-white rounded box-shadow">
                                <p>Total Record Count: {{ success.total }}</p>
                                <table class="table-records table">
                                    <thead>
                                        <tr>
                                            <th v-if="showRecordIndex" scope="col">#</th>
                                            <th v-if="showRecordKey">Key</th>
                                            <th>Value</th>
                                            <th v-if="showRecordTimestamp">Timestamp</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="(record, index) in success.records" v-bind:key="record.key">
                                            <th v-if="showRecordIndex" scope="row">{{ index + 1 }}</th>
                                            <td v-if="showRecordKey">{{ record.key }}</td>
                                            <td>
                                                 <template v-if="displayJsonValue">
                                                     <vue-json-pretty
                                                       :deep="1"
                                                       :data="record.value"
                                                       @click="handleClick">
                                                     </vue-json-pretty>
                                                 </template>
                                                 <template v-else>{{ record.value }}</template>
                                            </td>
                                            <td v-if="showRecordTimestamp">{{ record.timestamp }}</td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                         </div>
                    </template>
                    <template v-for="(failure, responseIndex) in failureResultsOrderByServer">
                        <div class="tab-pane fade"
                            v-bind:class="{ active: responseIndex == 0, show: responseIndex == 0 }"
                            v-bind:id="'nav-' + normalizeHtmlAttr(failure.server)"
                             v-bind:aria-labelledby="'nav-' + normalizeHtmlAttr(failure.server) + '-tab'"
                            role="tabpanel">
                            <template v-for="error in failure.errors">
                                <li class="list-group-item"><strong>Trace:</strong><br /> {{ error.message }}</li>
                            </template>
                         </div>
                    </template>
                </div>
            </div>
        </div>
    </div>
    <div class="col-4">
        <div id="query-col-form" class="bg-white box-shadow p-3">
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
                <div class="row mb-3 justify-content-md-center">
                    <div class="col-md-auto">
                        <button v-on:click.prevent="execute()"
                                v-bind:disabled="isActionBtnDisabled()" class="btn btn-primary">Execute</button>
                    </div>
                    <div class="col-md-auto">
                        <button v-on:click.prevent="copyAsCurl()"
                                 v-bind:disabled="isActionBtnDisabled()" class="btn btn-secondary">CopyAsCurl</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>
</div>
</template>
<script>
import azkarra from '../services/azkarra-api.js'
import VueJsonPretty from 'vue-json-pretty';
import _ from 'lodash';
import $ from 'jquery';
import * as datatables from "datatables.net-bs4";
import 'datatables.net-bs4/css/dataTables.bootstrap4.min.css';

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
  components: {
    'vue-json-pretty' : VueJsonPretty,
  },
  data: function () {
    return {
      showRecordKey: true,
      showRecordIndex: true,
      showRecordTimestamp: true,
      displayJsonValue: false,
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
        { typeLabel : 'TimestampedKeyValue', typeValue :'timestamped_key_value', operations : keyValueOperations, isTimestamped: true},
        { typeLabel : 'Window', typeValue :'window', operations : windowOperations },
        { typeLabel : 'TimestampedWindow', typeValue :'window', operations : windowOperations, isTimestamped: true},
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
  updated( ) {
     $('.table-records').DataTable();
  },
  computed: {
    successResultsOrderByServer: function () {
      return _.orderBy(this.response.result.success, 'server');
    },

    failureResultsOrderByServer: function () {
      return _.orderBy(this.response.result.failure, 'server');
    }
  },
  methods: {
    fetchLocalActiveStreams() {
      let that = this;
      azkarra.fetchLocalActiveStreamsList().then(function(data){
        that.applications = data
      })
    },

    fetchApplicationMetadata() {
      let that = this;
      azkarra.fetchApplicationMetadata({id: this.query.application }).then(function(data){
        let storeNames = new Set();
        data.forEach(server => {
          server.stores.forEach(s => storeNames.add(s));
          that.applicationServers.push(server.host + ":" + server.port);
        });
        that.stateStoreNames = storeNames;
      });
    },

    copyAsCurl() {
        // do quick copy to clipboard
        var input = document.createElement('input');
        input.setAttribute('value', azkarra.getQueryStateStoreAsCurl(this.buildQuery()));
        document.body.appendChild(input);
        input.select();
        var result = document.execCommand('copy');
        document.body.removeChild(input);
        return result;
    },

    normalizeHtmlAttr(str) {
        return str.replace(/(\.|:)/g, '-');
    },

    execute() {
      let that = this;
      that.response = {};
      that.showRecordTimestamp = that.isTimestampedStore();
      azkarra.sendQueryStateStore(that.buildQuery()).then(function(data){
        that.response = data
      });
    },

    isActionBtnDisabled() {
        return !(this.query.store &&
           this.query.application &&
           this.query.operation &&
           this.query.type);
    },

    isTimestampedStore() {
        return this.query.type && this.query.type.isTimestamped;
    },

    buildQuery() {
      return {
        params : this.query.params,
        type: this.query.type.typeValue,
        operation: this.query.operation.name,
        store: this.query.store,
        application : this.query.application,
        options: this.query.options
      };
    }
  }
}
</script>

<style scoped>
  #query-col-form {
    display: block;
    position: fixed;
  }
</style>



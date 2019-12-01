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
import axios from 'axios'
import moment from 'moment'

const apiBasePath = '/api/v1'

class AzkarraApi {

    constructor(client) {
      this.client = client;
      this.client.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';
    }

    axios() {
      return this.client;
    }

    setClientAuth(auth) {
      this.client.defaults.auth = auth;
    }

    getApi() {
      return this.client.get(apiBasePath)
      .then(function (response) {
        return response.data
      })
    }

    getVersion() {
      return this.client.get("/version")
      .then(function (response) {
        return response.data
      })
    }

    getHealth() {
      return this.client.get("/health")
      .then(function (response) {
        return response.data
      })
    }

    getInfo() {
      return this.client.get("/info")
      .then(function (response) {
        return response.data
      })
    }

    fetchStreamsTopologyById(instance) {
      return this.client.get(apiBasePath + "/applications/" + instance.id + "/topology")
      .then(function (response) {
        return response.data
      })
    }

    getActiveStreamsById(instance) {
     return this.client.get(apiBasePath + "/streams/" + instance.id)
     .then(function (response) {
         var data = response.data
         data.state.since = moment(data.state.since, "YYYY-MM-DD[T]HH:mm:ss.SSS").fromNow();
         return data;
       })
    }

    createLocalStreams(data) {
      return this.client.post(apiBasePath + "/streams", data)
      .then(function (response) {
        return response.data
      })
    }

    deleteLocalStreamsById(instance) {
      return this.client.delete(apiBasePath + "/streams/" + instance.id);
    }

    restartLocalStreamsById(instance) {
     return this.client.post(apiBasePath + "/streams/" + instance.id + "/restart");
    }

    stopLocalStreamsById(instance, cleanup) {
     return this.client.post(apiBasePath + "/streams/" + instance.id + "/stop", {cleanup : cleanup});
    }

    fetchStreamsMetricsById(instance) {
     return this.client.get(apiBasePath + "/streams/" + instance.id + "/metrics")
     .then(function (response) {
         var data = response.data
         var results = []
         data.forEach((group) => {
            var metrics = group.metrics.map((metric) => {
              return {
                name : metric.name,
                group : group.name,
                tags : metric.tags,
                description : metric.description,
                value: metric.value
              }
            })
            results = results.concat(metrics)
         })
         return results
         })
    }

    fetchLocalActiveStreamsList() {
      var that = this
        return this.client.get(apiBasePath + '/streams')
          .then(function (response) {
            return response.data;
           })
    }

    fetchLocalActiveStreamsConfig(instance) {
     var that = this
     return this.client.get(apiBasePath + '/streams/' + instance.id + "/config")
       .then(function (response) {
            return response.data;
       })
    }

    fetchLocalActiveStreamsStatusConfig(instance) {
     var that = this
     return this.client.get(apiBasePath + '/streams/' + instance.id + "/status")
       .then(function (response) {
            return response.data;
       })
    }

    fetchApplicationMetadata(instance) {
     var that = this
     return this.client.get(apiBasePath + '/applications/' + instance.id)
       .then(function (response) {
            return response.data;
       })
    }

    fetchLocalActiveStreams() {
     var that = this
     return this.client.get(apiBasePath + '/streams')
       .then(function (response) {
         var streams = []
         response.data.forEach( (id) => {
           that.getActiveStreamsById( {id: id} )
           axios.get(apiBasePath + '/streams/' + id).then(function (response) {
             var data = response.data;
             data.configLoaded = false;
             data.config = [];
             data.displayConfig = false;
             data.sinceHowLong = moment(data.since, "YYYY-MM-DD[T]HH:mm:ss.SSS").fromNow();
             data.state.since = moment(data.state.since, "YYYY-MM-DD[T]HH:mm:ss.SSS").fromNow();
             streams.push(data)
           })
         })
         return streams;
       })
    }

    fetchContext() {
     var that = this
     return this.client.get(apiBasePath + '/context')
       .then(function (response) {
         return response.data;
       })
    }

    fetchEnvironments() {
     var that = this
     return this.client.get(apiBasePath + '/environments')
       .then(function (response) {
         return response.data;
       })
    }

    createEnvironment(data) {
     return this.client.post(apiBasePath + '/environments', data);
    }


    fetchAvailableTopologies() {
     var that = this
     return this.client.get(apiBasePath + '/topologies')
       .then(function (response) {
         return response.data;
       })
    }

    sendQueryStateStore(query) {
      var that = this
      let url = apiBasePath + '/applications/' + query.application + "/stores/" + query.store;
      let data = {set_options: query.options, type: query.type, query : { [query.operation] : query.params } };
      return this.client.post(url, data)
        .then(function (response) {
          return response.data;
      })
    }
}

// Create the default instance to be exported
const azkarra = new AzkarraApi(axios);
export default azkarra;
/*
* Copyright 2019-2021 StreamThoughts.
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
  <div id="component-streams-list-container" class="container-fluid">
    <div class="panel border bg-white rounded box-shadow">
      <div class="panel-heading">Kafka Streams Instances</div>
      <div class="panel-body border-0">
        <table class="table">
          <thead>
          <tr>
            <th class="width-40"></th>
            <th>Application ID</th>
            <th>Name</th>
            <th>Version</th>
            <th>Endpoint</th>
            <th>Status</th>
            <th>Since</th>
          </tr>
          </thead>
          <tbody>
          <template v-for="instance in orderedInstances">
            <tr>
              <td class="width-40">
                <span v-bind:class="computeStateClass(instance.state.state)">
                    <i aria-hidden="true" class="fa fa-circle"></i>
                </span>
              </td>
              <td>
                <router-link :to="{path: '/applications/' + instance.id}">{{ instance.id }}</router-link>
              </td>
              <td>
                <router-link :to="{path: '/streams/' + instance.containerId}">{{ instance.name }}</router-link>
              </td>
              <td>{{ instance.version }}</td>
              <td>
                <a target="_blank"
                   :href="protocol + '//' +  instance.endpoint.address  + ':' + instance.endpoint.port + '/ui' ">
                  {{ instance.endpoint.address }}:{{ instance.endpoint.port }}
                </a>
              </td>
              <td>{{ instance.state.state }}</td>
              <td>{{ instance.since }}</td>
            <tr>
          </template>
          <tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import streamApiV1 from '../services/streams.api.js';
import {fromNow, getClassForHealthState} from '@/utils';

export default {
  data: function () {
    return {
      instances: [],
      protocol: window.location.protocol
    }
  },
  created() {
    this.load()
  },
  watch: {
    '$route': 'load'
  },
  methods: {
    load() {
      let that = this;
      streamApiV1.getAllInstancesIds().then(response => {
        response.data.forEach(id => {
          streamApiV1.getInstanceById(id).then(response => {
            let data = response.data;
            data.containerId = id;
            data.since = data.since = fromNow(data.state.since);
            that.instances.push(data);
          });
        });
      });
    },
    computeStateClass: function (state) {
      return getClassForHealthState(state)
    }
  },
  computed: {
    orderedInstances: function () {
      return _.orderBy(this.instances, 'id', 'containerId')
    },
  }
}
</script>


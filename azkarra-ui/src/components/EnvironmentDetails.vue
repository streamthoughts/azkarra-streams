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
  <div id="environment-details" class="container-fluid">
    <div class="row">
      <div class="col">
        <div class="panel border bg-white rounded box-shadow">
          <div class="panel-heading">Environment Information</div>
          <div class="panel-body">
            <div class="property-card-container">
              <div class="property-card">
                <div class="property-card-name">Name</div>
                <div class="property-card-value">{{ environment.name }}</div>
              </div>
              <div class="property-card">
                <div class="property-card-name">Type</div>
                <div class="property-card-value">{{ environment.type }}</div>
              </div>
              <div class="property-card">
                <div class="property-card-name">Default</div>
                <div class="property-card-value">{{ environment.is_default }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <div class="panel border bg-white rounded box-shadow">
          <div class="panel-heading">Applications</div>
          <div class="panel-body border-0">
            <table class="table">
              <thead>
              <tr>
                <th class="width-40"></th>
                <th>Name</th>
              </tr>
              </thead>
              <tbody>
              <template v-for="app in environment.applications">
                <tr>
                  <td class="width-40"></td>
                  <td>
                    <router-link :to="{path: '/applications/' + app}">{{ app }}</router-link>
                  </td>
                <tr>
              </template>
              <tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <div class="panel border bg-white rounded box-shadow">
          <div class="panel-heading">Configuration</div>
          <div class="panel-body border-0">
            <table class="table">
              <thead>
              <tr>
                <th class="width-40"></th>
                <th>Property</th>
                <th>Value</th>
              </tr>
              </thead>
              <tbody>
              <template v-for="(value, key) in environment.config" :key="key">
                <tr>
                  <td class="width-40"></td>
                  <td>{{ key }}</td>
                  <td>{{ value }}</td>
                <tr>
              </template>
              <tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import environmentApiV1 from '../services/environment.api.js';

export default {
  components: {},
  data: function () {
    return {
      name: '',
      environment: {}
    }
  },
  created() {
    this.name = this.$route.params.name;
    this.load();
  },
  watch: {
    '$route': 'load'
  },
  methods: {
    load() {
      let that = this;
      environmentApiV1.getEnvironmentByName(this.name).then(response =>
          that.environment = response.data
      );
    },
    goto(event, path, id) {
      this.$router.push({path: `/path/${id}`});
    },
  },
}
</script>

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
  <div>
    <div class="container-fluid" id="overview">
      <div class="row">
        <div class="col-6">
          <div class="my-3 p-3 panel border bg-white rounded box-shadow">
            <div class="panel-heading">Info</div>
            <div class="panel-body">
              <vue-json-pretty
                  :path="'res'"
                  :data="info"
                  @click="handleClick">
              </vue-json-pretty>
            </div>
          </div>
        </div>
        <div class="col-6">
          <div class="my-3 p-3 panel border bg-white rounded box-shadow">
            <div class="panel-heading">
              Health
              <span class="float-right" v-bind:class="health.statusClass">
                {{ health.status }}
              </span>
            </div>
            <div id="tile-overview-health" class="panel-body">
              <template v-if="health.details">
                <health-indicators-list v-bind:indicators="health.details"></health-indicators-list>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import azkarraApi from '../services/azkarra.api.js';
import VueJsonPretty from 'vue-json-pretty';
import { getClassForHealthState } from '@/utils';
import HealthIndicatorsList from './HealthIndicatorsList.vue';

export default {
  components: {
    'health-indicators-list': HealthIndicatorsList,
    'vue-json-pretty': VueJsonPretty,
  },
  data: function () {
    return {
      info: {},
      health: {},
    }
  },
  created() {
    this.load();
  },
  watch: {
    '$route': 'load',
  },
  methods: {
    load() {
      let that = this;
      azkarraApi.getHealth()
          .then(response => {
            that.health = response.data;
            that.health.statusClass = getClassForHealthState(that.health.status);
            that.health.applications.statusClass = getClassForHealthState(that.health.applications.status);
          })
          .catch(error => {
            if (error.data) that.health = error.data;
          });

      azkarraApi.getInfo().then(response => that.info = response.data);
    },
  },
}
</script>

<style scoped>
#tile-overview-health .table td, #tile-overview-health .table th {
  padding-right: 0;
}
</style>


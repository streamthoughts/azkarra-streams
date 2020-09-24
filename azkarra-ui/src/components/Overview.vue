/*
 * Copyright 2019-2020 StreamThoughts.
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
          <div class="my-3 p-3 bg-white rounded box-shadow">
            <h6 class="border-bottom border-gray pb-2 mb-3">Info</h6>
               <vue-json-pretty
                  :path="'res'"
                  :data="info"
                  @click="handleClick">
                </vue-json-pretty>
          </div>
        </div>
        <div class="col-6">
          <div id="tile-overview-health" class="my-3 p-3 bg-white rounded box-shadow">
            <h6 class="border-bottom border-gray pb-2 mb-3">Health
              <span class="float-right"
                v-bind:class="getClassForHealthStatus(health.status)">
                {{ health.status }}
              </span>
            </h6>
            <template v-if="health.details">
              <health-indicators-list v-bind:indicators="health.details"></health-indicators-list>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import azkarra from '../services/azkarra-api.js'
import VueJsonPretty from 'vue-json-pretty'
import HealthIndicatorsList from './HealthIndicatorsList.vue'

export default {
  components: {
    'health-indicators-list' : HealthIndicatorsList,
    'vue-json-pretty' : VueJsonPretty,
  },
  data: function () {
    return {
      info: {},
      health: {},
    }
  },
  created () {
     this.load();
  },
  watch: {
    '$route': 'load',
  },
  methods: {
    load() {
        var that = this;
        azkarra.getHealth()
            .then(function(data){
                that.health = data;
            })
            .catch(function (error){
               if (error.data) {
                 that.health = error.data;
               }
            });
        azkarra.getInfo().then(function(data){ that.info = data; });
    },

    getClassForHealthStatus(status) {
        return status == 'UP' ? 'text-success' : status == 'DOWN' ? 'text-danger' : 'text-warning';
    }
  },
}
</script>

<style scoped>
    #tile-overview-health .table td, #tile-overview-health .table th {
        padding-right: 0;
    }
</style>


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
  <div class="health-indicators">
    <div class="health-indicator" v-if="indicators.applications">
      <div class="health-indicator-title">
        <span class="health-indicator-name">Streams Applications</span>
        <span class="health-indicator-status float-right"
          v-bind:class="getClassForHealthStatus(indicators.applications.status)">
          {{ indicators.applications.status }}
        </span>
      </div>
      <div v-if="indicators.applications.details">
        <template v-for="(detail, name) in indicators.applications.details">
          <div class="health-indicator">
            <div class="health-indicator-title">
                <span class="health-indicator-name">{{ name }}</span>
                <span class="health-indicator-status float-right"
                  v-bind:class="getClassForHealthStatus(detail.status)">
                  {{ detail.status }}
                </span>
            </div>
            <div class="health-indicator-details" v-if="detail.details">{{ detail.details }}</div>
          </div>
        </template>
      </div>
    </div>
    <template v-for="(indicator, name) in indicators">
      <div class="health-indicator" v-if="name != 'applications'">
        <div class="health-indicator-title">
          <span class="health-indicator-name">{{ name }}</span>
          <span class="health-indicator-status float-right"
            v-bind:class="getClassForHealthStatus(indicator.status)">
            {{ indicator.status }}
          </span>
        </div>
        <div class="health-indicator-details" v-if="indicator.details">{{ indicator.details }}</div>
      </div>
    </template>
  </div>
</template>

<script>
export default {
  name: 'health-indicators-list',
  props: ['indicators' ],
  data: function () {
    return {
    }
  },
  methods: {
      getClassForHealthStatus(status) {
          return status == 'UP' ? 'text-success' : status == 'DOWN' ? 'text-danger' : 'text-warning';
      },
  }
}
</script>

<style scoped>
    .health-indicator {
        padding-left: 12px;
    }
    .health-indicator-name {
        font-size:1em;
        font-weight:600;
    }

    .health-indicator-title {
        border-bottom: 1px solid #dee2e6!important;
    }
</style>
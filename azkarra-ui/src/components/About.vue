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
  <div id="component-configuration-container">
    <div class="main-content-header">
      <h1 class="main-title">About</h1>
    </div>
    <div class="main-content-body container-fluid">
      <div class="row">
        <div class="col">
          <div class="panel border bg-white rounded box-shadow">
            <div class="panel-body">
              <div class="property-card-container">
                <div class="property-card">
                  <div class="property-card-name">Version</div>
                  <div class="property-card-value">Azkarra Dashboard {{ version.azkarraVersion }}</div>
                </div>
                <div class="property-card">
                  <div class="property-card-name">Git Branch</div>
                  <div class="property-card-value">{{ version.branch }}</div>
                </div>
                <div class="property-card">
                  <div class="property-card-name">Git Commit ID</div>
                  <div class="property-card-value">{{ version.commitId }}</div>
                </div>
                <div class="property-card">
                  <div class="property-card-name">License</div>
                  <div class="property-card-value">Apache License 2.0</div>
                </div>
              </div>
              <div class="about">
                <p>
                  Copyright 2019-2021 StreamThoughts</br>
                  Azkarra Dashboard is an <a href="https://github.com/streamthoughts/azkarra-streams" target="_blank">open
                  source project</a>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import azkarraApi from '../services/azkarra.api.js';

export default {
  components: {},
  data: function () {
    return {
      version: {},
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
      azkarraApi.getVersion().then(response => that.version = response.data);
    },
  },
}
</script>

<style scoped>
#tile-overview-health .table td, #tile-overview-health .table th {
  padding-right: 0;
}
</style>


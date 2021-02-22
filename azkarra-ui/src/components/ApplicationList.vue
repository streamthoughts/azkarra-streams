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
  <div id="applications-list" class="container-fluid">
    <div class="row">
      <div class="col">
        <div class="panel border bg-white rounded box-shadow">
          <div class="panel-heading">Kafka Streams Applications</div>
          <table class="table">
            <thead>
            <tr>
              <th></th>
              <th>Application ID</th>
              <th>Environment</th>
              <th>Instances</th>
            </tr>
            </thead>
            <tbody>
            <template v-for="app in orderedApplications" :key="app.id">
              <tr>
                <td></td>
                <td><router-link :to="{path: '/applications/' + app.id}">{{ app.id }}</router-link></td>
                <td><router-link :to="{path: '/environments/' + app.environment}">{{ app.environment }}</router-link></td>
                <td>{{ app.instances.length }}</td>
              </tr>
            </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import applicationApiV1 from '../services/application.api.js'

export default {
  components: {},
  data: function () {
    return {
      applications: [],
    }
  },
  created() {
    this.load();
  },
  watch: {
    '$route': 'load'
  },
  methods: {
    load() {
      let that = this;
      applicationApiV1.getAllApplications().then(resp1 => {
        resp1.data.forEach(id => {
          applicationApiV1.getApplicationById(id).then(resp2 => that.applications.push(resp2.data));
        });
      });
    },
  },
  computed: {
    orderedApplications: function () {
      return _.orderBy(this.applications, 'id')
    }
  }
}
</script>


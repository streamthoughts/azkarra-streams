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
<div id="local-streams-metrics">
  <table id="streams-metrics-table" class="table table-striped">
      <thead class="thead-dark">
          <tr>
              <th>Group</th>
              <th>Name</th>
              <th>Description</th>
              <th>Tags</th>
              <th>Value</th>
          </tr>
      </thead>
      <tbody>
          <span class="text-no-data" v-if="metrics == 0">No information available</span>
          <template v-for="metric in metrics">
              <tr>
                  <td>{{ metric.group }}</td>
                  <td>{{ metric.name }}</td>
                  <td>{{ metric.description }}</td>
                  <td>{{ metric.tags }}</td>
                  <td>{{ metric.value }}</td>
              </tr>
          </template>
      </tbody>
    </table>
</div>
</template>
<script>
import azkarra from '../services/azkarra-api.js'
import $ from 'jquery'
import * as datatables from "datatables.net-bs4";
import 'datatables.net-bs4/css/dataTables.bootstrap4.min.css';

export default {
  props: ["id"],
  data: function () {
    return {
      metrics: []
    }
  },
  created () {
     this.load()
  },
  watch: {
    'id': 'build',
  },
  updated( ) {
    $('#streams-metrics-table').DataTable();
  },
  methods: {
    load() {
        var that = this;
        azkarra
          .fetchStreamsMetricsById({id: this.id })
          .then(function(data){ that.metrics = data })
    }
  }
}
</script>
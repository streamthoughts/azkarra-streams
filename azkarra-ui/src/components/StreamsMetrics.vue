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
  <div id="component-streams-metrics-content" class="panel border bg-white rounded box-shadow">
    <div class="panel-heading">Kafka Streams Metrics</div>
    <div class="panel-body border-0">
      <p class="text-no-data" v-if="metrics.length === 0">No metrics available</p>
      <table id="streams-metrics-table"
             class="table table-striped"
             v-if="metrics.length > 0"
      >
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
  </div>
</template>
<script>
import streamApiV1 from '../services/streams.api.js';
import $ from 'jquery';
import * as datatables from "datatables.net-bs4";
import 'datatables.net-bs4/css/dataTables.bootstrap4.min.css';

export default {
  props: ["id"],
  data: function () {
    return {
      metrics: []
    }
  },
  created() {
    this.load()
  },
  watch: {
    'id': 'build',
  },
  updated() {
    $('#streams-metrics-table').DataTable();
  },
  methods: {
    load() {
      let that = this;
      streamApiV1.getInstanceMetricsById(that.id).then(response => {
        let data = response.data
        let results = []
        data.forEach((group) => {
          let metrics = group.metrics.map(metric => {
            return {
              name: metric.name,
              group: group.name,
              tags: metric.tags,
              description: metric.description,
              value: metric.value
            }
          });
          results = results.concat(metrics)
        });
        that.metrics = results;
      });
    }
  }
}
</script>
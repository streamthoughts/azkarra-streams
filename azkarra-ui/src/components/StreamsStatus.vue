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
  <div id="component-streams-tasks-content">
    <template v-if="threads.length === 0">
      <div class="panel border bg-white rounded box-shadow">
        <div class="panel-heading">Stream Threads</div>
        <div class="panel-body">
          <span class="text-no-data">No information available</span>
        </div>
      </div>
    </template>
    <div v-for="(thread) in threads" :key="thread.id" v-bind:id="'task-panel-'+thread.id">
      <div class="panel border bg-white rounded box-shadow">
        <div class="panel-heading">
          <i v-if="thread.state === 'FAILED'" class="fa fa-circle text-danger" aria-hidden="true"></i>
          <i v-else-if="thread.state === 'RUNNING'" class="fa fa-circle text-success" aria-hidden="true"></i>
          <i v-else class="fa fa-circle text-warning" aria-hidden="true"></i>
          Stream Thread ({{ thread.id }})
        </div>
      </div>
      <div class="ml-5 panel border bg-white rounded box-shadow">
        <div class="panel-body">
          <div class="property-card-container">
            <div class="property-card">
              <div class="property-card-name">Name</div>
              <div class="property-card-value">{{ thread.name }}</div>
            </div>
            <div class="property-card">
              <div class="property-card-name">State</div>
              <div class="property-card-value">{{ thread.state }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="ml-5 panel border bg-white rounded box-shadow">
        <div class="panel-heading">Active Tasks</div>
        <table class="table">
          <thead>
          <tr>
            <th scope="col">TaskID</th>
            <th scope="col">Topics/Partitions</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="thread in thread.active_tasks" :key="thread.task_id">
            <td>{{ thread.task_id }}</td>
            <td>{{ thread.topic_partitions }}</td>
          <tr>
          </tbody>
        </table>
      </div>
      <div class="ml-5 panel border bg-white rounded box-shadow">
        <div class="panel-heading">Standby Tasks</div>
        <div class="panel-body text-no-data" v-if="thread.standby_tasks.length === 0">
          No standby task
        </div>
        <table class="table" v-if="thread.standby_tasks.length > 0">
          <thead>
          <tr>
            <th scope="col">TaskID</th>
            <th scope="col">Topics/Partitions</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="thread in thread.standby_tasks" :key="thread.task_id">
            <td>{{ thread.task_id }}</td>
            <td>{{ thread.topic_partitions }}</td>
          <tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import streamApiV1 from '../services/streams.api.js';

export default {
  props: ["id"],
  data: function () {
    return {
      threads: []
    }
  },
  created() {
    this.load()
  },
  watch: {
    'id': 'build',
  },
  methods: {
    load() {
      var that = this;
      streamApiV1.getInstanceStatusById(this.id).then(response => {
        that.threads = response.data.threads;
        that.threads.forEach(thread => thread.id = thread.name.split("-").pop());
      });
    },
  },
  computed: {
    orderedThreads: function () {
      return _.orderBy(this.threads, 'id')
    }
  }
}
</script>
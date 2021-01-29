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
  <div id="streams-configs-content" class="panel border bg-white rounded box-shadow">
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
        <template v-for="(value, key) in configs" :key="key">
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
</template>

<script>
import streamApiV1 from '../services/streams.api.js'

export default {
  props: ["id"],
  data: function () {
    return {
      configs: {}
    }
  },
  created() {
    this.load()
  },
  watch: {
    'id': 'build',
  },
  methods: {
    load(streams) {
      let that = this;
      return streamApiV1.getInstanceConfigById(this.id).then(response => that.configs = response.data);
    },
  }
}
</script>
/*
* Copyright 2021 StreamThoughts.
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
  <div id="component-streams-states">
    <div class="row justify-content-between align-items-center">
      <div class="col">
        <div class="fl-right">
          <button v-on:click.prevent="load()"
                  type="button"
                  class="btn btn-primary">
            <i class="fas fa-sync"></i> Refresh
          </button>
        </div>
      </div>
    </div>
    <template v-for="state in states" :key="state.name">
      <div class="panel border bg-white rounded box-shadow">
        <div class="panel-heading">Store : {{ state.name }}</div>
        <div class="panel-body border-0">
          <table class="table">
            <thead>
            <tr class="d-flex">
              <th class="col-md-2">Topic</th>
              <th class="col-md-1">Partition</th>
              <th class="col-md-2">
                <a data-toggle="tooltip" data-placement="top"
                   title="The total number of records that has been successfully restored for this store partition replica.">
                  Total Restored Records <i class="fas fa-info-circle"></i>
                </a>
              </th>
              <th class="col-md-2">
                <a data-toggle="tooltip" data-placement="top"
                   title="The current maximum offset on the store partition's changelog topic, that has been successfully written into the store partition's state store.">
                  Current Offset <i class="fas fa-info-circle"></i>
                </a>
              </th>
              <th class="col-md-2">
                <a data-toggle="tooltip" data-placement="top"
                   title="The end offset position for this store partition's changelog topic on the Kafka brokers.">
                  End Offset <i class="fas fa-info-circle"></i>
                </a>
              </th>
              <th class="col-md-3">
                <a data-toggle="tooltip" data-placement="top"
                   title="The measured lag between current and end offset positions, for this store partition replica.">
                  Lag <i class="fas fa-info-circle"></i>
                </a>
              </th>
            </tr>
            </thead>
            <tbody>
            <template v-for="offset in state.offsets" :key="offset.id">
              <tr class="d-flex">
                <td class="col-md-2">{{ offset.topic }}</td>
                <td class="col-md-1"><span class="badge badge-square badge-light">{{ offset.partition }}</span></td>
                <td class="col-md-2">{{ offset.total_restored }} (duration: {{ offset.duration }})</td>
                <td class="col-md-2">{{ offset.current_offset }}</td>
                <td class="col-md-2">{{ offset.log_end_offset }}</td>
                <td class="col-md-3">
                  <div style="text-align:center">{{ offset.offset_lag }}</div>
                  <div class="progress" style="height: 15px;">
                    <div class="progress-bar progress-bar-striped bg-warning"
                         role="progressbar"
                         v-bind:style="{ width: offset.restored_progress + '%' }"
                         :aria-valuenow="offset.restored_current_offset"
                         :aria-valuemin="offset.restored_starting_offset"
                         :aria-valuemax="offset.restored_ending_offset">
                    </div>
                    <div class="progress-bar"
                         role="progressbar"
                         v-bind:style="{ width: offset.progress + '%' }"
                         :aria-valuenow="offset.current_offset"
                         :aria-valuemax="offset.log_end_offset"
                         :aria-valuemin="offset.restored_ending_offset">
                    </div>
                  </div>
                </td>
              <tr>
            </template>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import streamApiV1 from '../services/streams.api.js';
import $ from 'jquery'
import _ from 'lodash';

const humanReadableFormat = (duration) => {
  return duration
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
}

export default {
  props: ["id"],
  data: function () {
    return {
      states: []
    }
  },
  created() {
    this.load()
  },
  watch: {
    'id': 'build',
  },
  updated() {
    $(function () {
      $('[data-toggle="tooltip"]').tooltip()
    })
  },
  methods: {
    load() {
      let that = this;
      streamApiV1.getInstanceStoresById(that.id).then(response => {
        let states = response.data.flatMap(state => {
          let restoreGroupedByPartition = _.keyBy(state['partition_restore_infos'], o => o.partition);
          let lagGroupedByPartition = _.keyBy(state['partition_lag_infos'], o => o.partition);

          let offsets = Object.entries(restoreGroupedByPartition).map(([partition, restored]) => {
            let currentOffset = lagGroupedByPartition[partition]['current_offset'];
            let logEndOffset = lagGroupedByPartition[partition]['log_end_offset'];
            let currentRestoredOffset = restored['starting_offset'] + restored['total_restored'];
            return {
              topic: restored['topic'],
              partition: restored['partition'],
              restored_starting_offset: restored['starting_offset'],
              restored_ending_offset: restored['ending_offset'],
              restored_current_offset: currentRestoredOffset,
              restored_progress: currentRestoredOffset / (logEndOffset) * 100,
              total_restored: restored['total_restored'],
              duration: humanReadableFormat(restored['duration']),
              current_offset: currentOffset,
              log_end_offset: logEndOffset,
              offset_lag: lagGroupedByPartition[partition]['offset_lag'],
              progress: (currentOffset - currentRestoredOffset) / (logEndOffset) * 100
            };
          });
          return {name: state.name, offsets: offsets};
        });
        that.states = _.orderBy(states, 'name');
      });
    }
  }
}
</script>
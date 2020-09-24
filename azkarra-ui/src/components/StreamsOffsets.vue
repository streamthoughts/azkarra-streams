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
<div id="local-streams-offsets">
    <div class="row justify-content-between align-items-center">
         <div class="col">
             <p class="mb-1">This only displays consumer-lags for the topics and partitions currently assigned to this Kafka Streams instance.</p>
         </div>
         <div class="col-3">
             <div class="fl-right">
                 <button v-on:click.prevent="load()"
                         type="button"
                         class="btn btn-primary">
                     <i class="fas fa-sync"></i> Refresh
                 </button>
             </div>
         </div>
    </div>
    <template v-for="topic in topics" :key="topic.name">
      <div class="mb-3">
        <div class="tab-pane-title mb-1">
          Topic :  {{ topic.name }}
        </div>
        <div class="tab-pane-content bg-white rounded box-shadow bordered">
          <table class="table">
            <thead>
              <tr class="d-flex">
                <th class="col-md-2">Consumer Id</th>
                <th class="col-md-1">Partition</th>
                <th class="col-md-2">
                  <a data-toggle="tooltip" data-placement="top" title="The offset of the last message consumed by the consumer for a specific partition.">
                    Current Offset <i class="fas fa-info-circle"></i>
                  </a/>
                </th>
                <th class="col-md-2">
                  <a data-toggle="tooltip" data-placement="top" title="The last offset committed by the consumer for a specific partition.">
                    Committed Offset <i class="fas fa-info-circle"></i>
                  </a/>
                </th>
                <th class="col-md-2">End Offset</th>
                <th class="col-md-3">
                  <a data-toggle="tooltip" data-placement="top" title="The number of messages the consumer lags behind the producer by for a specific partition (i.e : EndOffset - CurrentOffset - 1)  ">
                    Lag <i class="fas fa-info-circle"></i>
                  </a/>
                </th>
              </tr>
            </thead>
            <tbody>
              <template v-for="offset in topic.offsets" :key="offset.id">
                <tr class="d-flex">
                  <td class="col-md-2">{{ offset.client_id }}</td>
                  <td class="col-md-1"><span class="badge badge-square badge-light">{{ offset.partition }}</span></td>
                  <td class="col-md-2">{{ offset.consumed_offset }}</td>
                  <td class="col-md-2">{{ offset.committed_offset }}</td>
                  <td class="col-md-2">{{ offset.log_end_offset }}</td>
                  <td class="col-md-3">
                    <div style="text-align:center">{{ offset.lag }} <span v-if="offset.lag > 0">(Estimated Lag Time : {{ offset.lag_time}})</span></div>
                    <div class="progress" style="height: 15px;">
                      <div class="progress-bar"
                        role="progressbar"
                        v-bind:style="{ width: offset.progress + '%' }"
                        :aria-valuenow="offset.consumed_offset"
                        :aria-valuemax="offset.log_end_offset"
                        :aria-valuemin="offset.log_start_offset">
                       </div>
                    </div
                   </td>
                <tr>
              </template>
            </tbody>
          </table>
        </div>
    </template>
    {{ offsets }}
</div>
</template>
<script>
import azkarra from '../services/azkarra-api.js'
import $ from 'jquery'
import moment from 'moment'
import _ from 'lodash';

export default {
  props: ["id"],
  data: function () {
    return {
      topics: []
    }
  },
  created () {
     this.load()
  },
  watch: {
    'id': 'build',
  },
  updated( ) {
      $(function () {
        $('[data-toggle="tooltip"]').tooltip()
      })
  },
  methods: {
    load() {
      var that = this;
      azkarra.fetchLocalActiveStreamsOffsets({id: this.id}).then(function(data) {
        let mapOffsetGroupedByTopic = data.consumers.flatMap(consumer => {
          return consumer.positions.flatMap(pos => {
            pos['client_id'] = consumer['client_id'];
            pos['stream_thread'] = consumer['stream_thread'];
            pos['id'] = consumer['stream_thread'] + '-' + pos['partition'];
            pos['progress'] = pos['consumed_offset'] / (pos['log_end_offset'] - 1) * 100;
            let consumed = moment.duration(pos['consumed_timestamp'], 'x');
            pos['lag_time'] = moment.duration(moment().subtract(consumed)).humanize();
            return pos;
          });
        })
        .reduce((map, offset) => {
            if (!map.has(offset.topic)) map.set(offset.topic, []);
            map.get(offset.topic).push(offset);
            return map;
          },
          new Map()
        );
        that.topics = _.orderBy(Array.from(mapOffsetGroupedByTopic).map(([key, value]) => ({ name: key, offsets : _.orderBy(value, 'id')}), 'name'));
      });
    }
  }
}
</script>
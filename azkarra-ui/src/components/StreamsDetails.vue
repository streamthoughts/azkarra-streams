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
  <div id="component-streams-details-container" class="container-fluid">
    <div class="row justify-content-between">
      <div class="col">
        <ul class="list-inline left-border" v-if="streams.state">
          <li class="list-inline-item">
            <span v-bind:class="getStreamStateClass">
              <i aria-hidden="true" class="fa fa-circle"></i>
            </span>
            {{ streams.name }}
          </li>
          <li class="list-inline-item">
            {{ streams.state.state }} ({{ streams.state.since }})
          </li>
        </ul>
      </div>
      <div class="col-3">
        <div class="dropdown fl-right">
          <button class="btn btn-dark dropdown-toggle"
                  type="button"
                  id="dropdownMenuButton" data-toggle="dropdown"
                  aria-haspopup="true" aria-expanded="false">
            Available Actions
          </button>
          <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
            <a class="dropdown-item" href="#" v-on:click="restart()">
              <i class="fas fa-redo"></i>Restart
            </a>
            <a class="dropdown-item" href="#" v-on:click="toggleConfirmStop()" v-if="isStoppable()">
              <i class="fas fa-stop"></i>Stop
            </a>
            <a class="dropdown-item" href="#" v-on:click="toggleConfirmDelete()">
              <i class="fas fa-trash-alt"></i>Terminate
            </a>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <nav>
          <div class="nav nav-tabs" id="nav-tab" role="tablist">
            <a class="nav-item nav-link active"
               id="nav-streams-overview-tab"
               data-toggle="tab"
               href="#nav-streams-overview" role="tab"
               aria-controls="nav-streams-overview"
               aria-selected="true">
              Overview
            </a>
            <a class="nav-item nav-link"
               id="nav-streams-dag-tab"
               data-toggle="tab"
               href="#nav-streams-dag" role="tab"
               aria-controls="nav-streams-dag"
               aria-selected="false">
              DAG
            </a>
            <a class="nav-item nav-link"
               id="nav-streams-tasks-tab"
               data-toggle="tab"
               href="#nav-streams-tasks" role="tab"
               aria-controls="nav-streams-tasks"
               aria-selected="false">
              Threads/Tasks
            </a>
            <a class="nav-item nav-link"
               id="nav-streams-config-tab"
               data-toggle="tab"
               href="#nav-streams-config"
               role="tab" aria-controls="nav-streams-config"
               aria-selected="false">
              Configuration
            </a>
            <a class="nav-item nav-link"
               id="nav-streams-metrics-tab"
               data-toggle="tab"
               href="#nav-streams-metrics"
               role="tab"
               aria-controls="nav-streams-metrics"
               aria-selected="false">
              Metrics
            </a>
            <a class="nav-item nav-link"
               id="nav-streams-offsets-tab"
               data-toggle="tab"
               href="#nav-streams-offsets"
               role="tab"
               aria-controls="nav-streams-offsets"
               aria-selected="false">
              Consumers/Offsets
            </a>
            <template v-if="streams.exception">
              <a class="nav-item nav-link"
                 id="nav-streams-trace-tab"
                 data-toggle="tab"
                 href="#nav-streams-error-trace"
                 role="tab"
                 aria-controls="nav-streams-error-trace"
                 aria-selected="false">
                Error Trace
              </a>
            </template>
          </div>
        </nav>
        <div class="tab-content" id="nav-tabContent">
          <div class="tab-pane fade show active" id="nav-streams-overview" role="tabpanel"
               aria-labelledby="nav-streams-overview-tab">
            <div class="panel border bg-white rounded box-shadow">
              <div class="panel-heading">Instance</div>
              <div class="panel-body">
                <div class="property-card-container">
                  <div class="property-card">
                    <div class="property-card-name">Application ID</div>
                    <div class="property-card-value">
                      <router-link :to="{path: '/applications/' + streams.id}">{{ streams.id }}</router-link>
                    </div>
                  </div>
                  <div class="property-card">
                    <div class="property-card-name">Version</div>
                    <div class="property-card-value">{{ streams.version }}</div>
                  </div>
                  <div class="property-card">
                    <div class="property-card-name">Endpoint</div>
                    <div class="property-card-value">
                        <a target="_blank"
                           :href="protocol + '//' +  streams.endpoint.address  + ':' + streams.endpoint.port + '/ui' ">
                          {{ streams.endpoint.address }}:{{ streams.endpoint.port }}
                        </a>
                    </div>
                  </div>
                  <div class="property-card">
                    <div class="property-card-name">Started</div>
                    <div class="property-card-value">{{ streams.since }}</div>
                  </div>
                  <div class="property-card">
                    <div class="property-card-name">Description</div>
                    <div class="property-card-value">{{ streams.description }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="panel border bg-white rounded box-shadow">
              <div class="panel-heading">Topics</div>
              <div class="panel-body border-0">
                <table class="table">
                  <thead>
                  <tr>
                    <th class="width-40"></th>
                    <th>Name</th>
                    <th>Partition</th>
                  </tr>
                  </thead>
                  <tbody>
                  <template v-for="assignment in orderedTopicAssignments">
                    <tr>
                      <td class="width-40"></td>
                      <td>{{ assignment.topic }}</td>
                      <td>{{ assignment.partition }}</td>
                    <tr>
                  </template>
                  <tbody>
                </table>
              </div>
            </div>
            <div class="panel border bg-white rounded box-shadow">
              <div class="panel-heading">Standby Topics</div>
              <div class="panel-body border-0">
                <table class="table">
                  <thead>
                  <tr>
                    <th class="width-40"></th>
                    <th>Name</th>
                    <th>Partition</th>
                  </tr>
                  </thead>
                  <tbody>
                  <template v-for="assignment in orderedStandbyAssignments">
                    <tr>
                      <td class="width-40"></td>
                      <td>{{ assignment.topic }}</td>
                      <td>{{ assignment.partition }}</td>
                    <tr>
                  </template>
                  <tbody>
                </table>
              </div>
            </div>
          </div>
          <div class="tab-pane fade" id="nav-streams-dag" role="tabpanel" aria-labelledby="nav-streams-dag-tab">
            <streams-topology v-bind:id="id"></streams-topology>
          </div>
          <div class="tab-pane fade" id="nav-streams-tasks" role="tabpanel" aria-labelledby="nav-streams-tasks-tab">
            <streams-status v-bind:id="id"></streams-status>
          </div>
          <div class="tab-pane fade" id="nav-streams-config" role="tabpanel" aria-labelledby="nav-streams-config-tab">
            <streams-config v-bind:id="id"></streams-config>
          </div>
          <div class="tab-pane fade" id="nav-streams-metrics" role="tabpanel" aria-labelledby="nav-streams-metrics-tab">
            <streams-metrics v-bind:id="id"></streams-metrics>
          </div>
          <div class="tab-pane fade" id="nav-streams-offsets" role="tabpanel" aria-labelledby="nav-streams-offsets-tab">
            <streams-offsets v-bind:id="id"></streams-offsets>
          </div>
          <div class="tab-pane fade" id="nav-streams-error-trace" role="tabpanel"
               aria-labelledby="nav-streams-metrics-tab">
            <div class="panel border bg-white rounded box-shadow">
              <div class="panel-heading">StackTrace</div>
              <div class="panel-body">{{ streams.exception }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <vue-modal v-if="openConfirmModal">
      <template v-slot:header>
        <h3>Are you absolutely sure?</h3>
      </template>
      <template v-slot:body>
        <form>
          <div class="form-check" v-if="isStopAction()">
            <div class="alert alert-warning" role="alert">
              Checking this box will do a clean up of the local
              StateStore directory (StreamsConfig.STATE_DIR_CONFIG) by deleting all data with regard to the application
              ID.
            </div>
            <input class="form-check-input"
                   type="checkbox" value=""
                   id="stopConfirmCleanup"
                   v-model="formConfirmStop.cleanup">
            <label class="form-check-label" for="stopConfirmCleanup">Enable cleanup</label>
          </div>
          <div class="alert alert-danger" role="alert" v-if="isDeleteAction()">
            This action cannot be undone! The streams instance will be stopped and removed from its environment.
            Please also keep in mind that: Terminating an instance will delete all local data with regard
            to the application.
          </div>
          <div class="form-group">
            <label for="confirmStreamsApplication">Please type in the id of the instance to confirm.</label>
            <input type="text"
                   class="form-control" id="confirmStreamsApplication"
                   aria-describedby="confirmStreamsApplicationHelp"
                   v-model="formConfirmStop.id">
            <small id="confirmStreamsApplicationHelp" class="form-text text-muted">
              The application to stop is '{{ id }}'.
            </small>
          </div>
        </form>
      </template>
      <template v-slot:footer>
        <button class="btn btn-dark" v-on:click="toggleConfirmStop()">Close</button>
        <button class="btn btn-danger"
                v-bind:disabled="!isConfirmed()"
                v-on:click="confirm()"
                id="stop-btn">{{ actionToConfirm }}
        </button>
      </template>
    </vue-modal>
  </div>
</template>

<script>

import axios from 'axios';
import streamApiV1 from '../services/streams.api.js';
import {fromNow, getClassForHealthState} from '@/utils';
import StreamsTopology from './StreamsTopology.vue';
import StreamsMetrics from './StreamsMetrics.vue';
import StreamsConfig from './StreamsConfig.vue';
import StreamsOffsets from './StreamsOffsets.vue';
import StreamsStatus from './StreamsStatus.vue';
import VueModal from './VueModal.vue';
import VueJsonPretty from 'vue-json-pretty';

const ActionTerminate = "Terminate";
const ActionStop = "Stop";

export default {
  components: {
    'streams-topology': StreamsTopology,
    'streams-metrics': StreamsMetrics,
    'streams-config': StreamsConfig,
    'streams-offsets': StreamsOffsets,
    'streams-status': StreamsStatus,
    'vue-json-pretty': VueJsonPretty,
    'vue-modal': VueModal,
  },
  data: function () {
    return {
      id: "",
      streams: {endpoint: {}},
      metadata: {assignments: [], standbyAssignments: []},
      status: {},
      offsets: {},
      openConfirmModal: false,
      formConfirmStop: {
        cleanup: false,
        id: "",
      },
      actionToConfirm: "",
      protocol: window.location.protocol,
    }
  },
  created() {
    this.id = this.$route.params.id;
    this.load();
    this.timer = setInterval(this.load, 5000);
  },

  watch: {
    '$route': 'load',
  },

  beforeDestroy() {
    clearInterval(this.timer);
  },

  methods: {
    load() {
      let that = this;
      axios.all([
        streamApiV1.getInstanceById(that.id),
        streamApiV1.getInstanceMetadataById(that.id)
      ])
          .then(axios.spread((streams, metadata) => {
            that.streams = streams.data;
            that.streams.state.since = fromNow(that.streams.state.since);
            that.metadata = metadata.data;
          }));
    },

    restart() {
      streamApiV1.restartInstanceById(this.id);
    },

    toggleConfirmStop() {
      this.openConfirmModal = !this.openConfirmModal;
      if (this.openConfirmModal) {
        this.actionToConfirm = ActionStop;
      }
    },

    toggleConfirmDelete() {
      this.openConfirmModal = !this.openConfirmModal;
      if (this.openConfirmModal) {
        this.actionToConfirm = ActionTerminate;
      }
    },

    isConfirmed() {
      return this.formConfirmStop.id === this.id;
    },

    isStoppable() {
      if (this.streams.state) {
        return this.streams.state.state !== 'FAILED' && this.streams.state.state !== 'NOT_RUNNING';
      } else {
        return false;
      }
    },

    confirm() {
      if (this.isConfirmed()) {
        if (this.isStopAction()) {
          streamApiV1.stopInstanceById(this.id, this.formConfirmStop.cleanup);
        }

        if (this.isDeleteAction()) {
          streamApiV1.deleteInstanceById(this.id).then(() =>
              this.$router.push({path: `/applications`})
          )
        }
        this.closeConfirm();
      }
    },

    isDeleteAction() {
      return this.actionToConfirm === ActionTerminate;
    },

    isStopAction() {
      return this.actionToConfirm === ActionStop;
    },

    closeConfirm() {
      this.openConfirmModal = false;
      this.actionToConfirm = '';
      this.formConfirmStop = {};
    },
  },
  computed: {
    getStreamStateClass: function () {
      if (this.streams.state) {
        return getClassForHealthState(this.streams.state.state);
      } else {
        return 'text-warning';
      }
    },
    orderedTopicAssignments: function () {
      let items = this.metadata.assignments.flatMap(function (assignment) {
        return assignment.partitions.map(function (partition) {
          return {topic: assignment.name, partition: partition};
        });
      });
      return _.orderBy(items, 'topic', 'partition')
    },
    orderedStandbyAssignments: function () {
      let items = this.metadata.standbyAssignments.flatMap(function (assignment) {
        return assignment.partitions.map(function (partition) {
          return {topic: assignment.name, partition: partition};
        });
      });
      return _.orderBy(items, 'topic', 'partition')
    },
  }
}
</script>

<style scoped>
.list-inline .list-inline-item:first-child {
  font-size: 1.4em;
  font-weight: 600;
}

.svg-inline--fa {
  height: 0.9em;
  padding-right: 5px;
}
</style>
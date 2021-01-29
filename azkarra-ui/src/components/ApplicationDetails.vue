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
  <div id="application-details" class="container-fluid">
    <div class="row justify-content-end my-3">
      <div class="col text-right">
        <div class="dropdown action-btn-right">
          <button class="btn btn-dark dropdown-toggle"
                  type="button"
                  id="dropdownMenuButton" data-toggle="dropdown"
                  aria-haspopup="true" aria-expanded="false">
            Available Actions
          </button>
          <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
            <a class="dropdown-item" href="#" v-on:click="toggleConfirmTerminate()">
              <i class="fas fa-trash-alt"></i>Terminate
            </a>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <div class="panel border bg-white rounded box-shadow">
          <div class="panel-heading">Information</div>
          <div class="panel-body">
            <div class="property-card-container">
              <div class="property-card">
                <div class="property-card-name">Application ID</div>
                <div class="property-card-value">{{ application.id }}</div>
              </div>
              <div class="property-card">
                <div class="property-card-name">Environment</div>
                <div class="property-card-value">
                  <router-link :to="{path: '/environments/' + application.environment}">{{ application.environment }}</router-link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <div class="panel border bg-white rounded box-shadow">
          <div class="panel-heading">Instances</div>
          <div class="panel-body border-0">
            <table class="table">
              <thead>
              <tr>
                <th class="width-40"></th>
                <th>Container ID</th>
                <th>Version</th>
                <th>Stores</th>
                <th>Endpoint</th>
                <th>Status</th>
              </tr>
              </thead>
              <tbody>
              <template v-for="instance in instances">
                <tr>
                  <td class="width-40">
                    <span v-bind:class="instance.stateClass"><i aria-hidden="true" class="fa fa-circle"></i></span>
                  </td>
                  <td>
                    <router-link :to="{path: '/streams/' + instance.id}">{{ instance.id }}</router-link>
                  </td>
                  <td>{{ instance.version }}</td>
                  <td>{{ instance.metadata.stores }}</td>
                  <td>
                    <a target="_blank"
                       :href="protocol + '//' +  instance.endpoint.address  + ':' + instance.endpoint.port + '/ui' ">
                      {{ instance.endpoint.address }}:{{ instance.endpoint.port }}
                    </a>
                  </td>
                  <td>{{ instance.state }}</td>
                <tr>
              </template>
              <tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <div class="panel border bg-white rounded box-shadow">
          <div class="panel-heading">Topics</div>
          <div class="panel-body border-0">
            <table class="table">
              <thead>
              <tr>
                <th class="width-40"></th>
                <th>Name</th>
                <th>Partition</th>
                <th>Assigned</th>
              </tr>
              </thead>
              <tbody>
              <template v-for="assignment in orderedTopicAssignments">
                <tr>
                  <td class="width-40"></td>
                  <td>{{ assignment.topic }}</td>
                  <td>{{ assignment.partition }}</td>
                  <td>
                    <a target="_blank"
                       :href="protocol + '//' +  assignment.endpoint.address  + ':' + assignment.endpoint.port + '/ui' ">
                      {{ assignment.endpoint.address }}:{{ assignment.endpoint.port }}
                    </a>
                  </td>
                <tr>
              </template>
              <tbody>
            </table>
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
          <div class="alert alert-danger" role="alert">
            This action cannot be undone! The streams instances will be stopped and removed from its environment.
            Please also keep in mind that: Terminating an application will delete all local data with regard
            to the application.
          </div>
          <div class="form-group">
            <label for="confirmStreamsApplication">Please type in the id of the application to confirm.</label>
            <input type="text"
                   class="form-control" id="confirmStreamsApplication"
                   aria-describedby="confirmStreamsApplicationHelp"
                   v-model="formConfirmTerminate.id">
            <small id="confirmStreamsApplicationHelp" class="form-text text-muted">
              The application to stop is '{{ id }}'.
            </small>
          </div>
        </form>
      </template>
      <template v-slot:footer>
        <button class="btn btn-dark" v-on:click="toggleConfirmTerminate()">Close</button>
        <button class="btn btn-danger"
                v-bind:disabled="!isTerminateConfirmed()"
                v-on:click="terminate()"
                id="stop-btn">Terminate
        </button>
      </template>
    </vue-modal>
  </div>
</template>

<script>
import applicationApiV1 from '../services/application.api.js'
import streamApiV1 from "@/services/streams.api";
import VueModal from './VueModal.vue';
import {fromNow, getClassForHealthState} from "@/utils";

export default {
  components: {
    'vue-modal': VueModal,
  },
  data: function () {
    return {
      id: "",
      application: {},
      assignments: [],
      instances: [],
      protocol: window.location.protocol,
      openConfirmModal: false,
      formConfirmTerminate: { id: "" },
    }
  },
  created() {
    this.id = this.$route.params.id;
    this.load();
  },
  watch: {
    '$route': 'load'
  },

  methods: {
    load() {
      let that = this;
      applicationApiV1.getApplicationById(this.id).then(response => {
        that.application = response.data;
        that.application.instances.forEach(instance => {
          if (!_.isUndefined(instance.id) && !_.isNull(instance.id)) {
            streamApiV1.getInstanceById(instance.id).then(response => {
              let data = response.data;
              instance.state = data.state.state;
              instance.stateClass = getClassForHealthState(instance.state);
              instance.version = data.version;
              instance.since = fromNow(data.since);
              that.instances.push(instance);
            });
          } else {
            instance.state = '-';
            instance.stateClass = getClassForHealthState(undefined);
            instance.version = '-';
            instance.since = '-';
            that.instances.push(instance);
          }
        });

        that.assignments = that.application.instances.flatMap(instance => {
          return instance.metadata.assignments.flatMap(assignment => {
            return assignment.partitions.map(partition => {
              return {endpoint: instance.endpoint, topic: assignment.name, partition: partition};
            });
          });
        });
      });
    },

    toggleConfirmTerminate() {
      this.openConfirmModal = !this.openConfirmModal;
    },

    isTerminateConfirmed() {
      return this.formConfirmTerminate.id === this.id;
    },

    terminate() {
      if (this.isTerminateConfirmed()) {
        applicationApiV1.deleteApplicationById(this.id).then(() => this.$router.push({path: `/applications`}));
        this.closeConfirm();
      }
    },

    goto(event, path, id) {
      this.$router.push({path: `/path/${id}`});
    },
  },
  computed: {
    orderedTopicAssignments: function () {
      return _.orderBy(this.assignments, 'topic', 'partition')
    },
  }
}
</script>

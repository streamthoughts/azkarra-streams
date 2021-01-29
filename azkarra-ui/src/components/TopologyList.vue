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
  <div id="component-topology-list-container" class="container-fluid">
    <div class="row">
      <div class="col">
        <div class="panel border bg-white rounded box-shadow">
          <div class="panel-heading">Registered Topologies</div>
          <table class="table">
            <thead>
            <tr>
              <th>Type</th>
              <th>Aliases</th>
              <th>Versions</th>
              <th></th>
            </tr>
            </thead>
            <tbody>
            <template v-for="topology in topologies" :key="topology.type">
              <tr>
                <td>{{ topology.type }}</td>
                <td>{{ topology.aliases }}</td>
                <td>
                  <select class="custom-select"
                          v-on:change="loadTopologyVersion($event, topology)">
                    <option selected disabled>Select a version</option>
                    <template v-for="version in topology.versions">
                      <option v-bind:value="version">{{ version }}</option>
                    </template>
                  </select>
                </td>
                <td>
                  <div class="btn-group" role="group" aria-label="operations">
                    <button type="button"
                            class="btn btn-primary"
                            v-bind:disabled="topology.disabled"
                            v-on:click="configure(topology)">Deploy
                    </button>
                  </div>
                </td>
              </tr>
            </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <vue-modal v-if="openCreateModal">
      <template v-slot:header>
        <h3>Deploy new streams application</h3>
      </template>
      <template v-slot:body>
        <form>
          <div class="form-group mb-3">
            <label for="type">Type</label>
            <input v-model="form.type" readonly type="text" class="form-control" id="type" aria-describedby="typeHelp">
          </div>
          <div class="form-group mb-3">
            <label for="version">Version</label>
            <input v-model="form.version" readonly type="text" class="form-control" id="version"
                   aria-describedby="versionHelp">
          </div>
          <div class="form-group mb-3">
            <label for="name">Name</label>
            <input v-model="form.name" type="text" class="form-control" id="name" aria-describedby="nameHelp">
            <small id="nameHelp" class="form-text text-muted">The name to identify this streams
              application(required).</small>
          </div>
          <div class="form-group mb-3">
            <label for="description">Description</label>
            <input v-model="form.description" type="text" class="form-control" id="description"
                   aria-describedby="descriptionHelp">
            <small id="descriptionHelp" class="form-text text-muted">The description for this streams
              application</small>
          </div>
          <div class="form-group mb-3">
            <label for="description">Environment</label>
            <select v-model="form.env" class="custom-select" id="environment" aria-describedby="envHelp">>
              <template v-for="env in environments">
                <option v-bind:value="env.name">{{ env.name }} (type: {{ env.type }})</option>
              </template>
            </select>
            <small id="envHelp" class="form-text text-muted">The environment to deploy this streams
              application(required).</small>
          </div>
          <div class="form-group mb-3">
            <label for="streamConfig">Configuration</label>
            <div aria-describedby="streamConfigHelp">
              <vue-json-editor
                  v-model="form.config"
                  @json-change="onJsonChange"
                  @has-error="onJsonError">
              </vue-json-editor>
            </div>
            <small id="streamConfigHelp" class="form-text text-muted">The JSON configuration for this streams
              application</small>
          </div>
        </form>
      </template>
      <template v-slot:footer>
        <button class="btn btn-primary" v-bind:disabled="disableModalDeployBtn()" v-on:click="deploy">Deploy</button>
        <button class="btn btn-dark" v-on:click="toggleModal()">Close</button>
      </template>
    </vue-modal>
  </div>
</template>
<script>
import topologyApiV1 from '../services/topology.api.js';
import streamsApiV1 from '../services/streams.api.js';
import environmentApiV1 from '../services/environment.api.js';
import VueModal from './VueModal.vue';
import VueJsonEditor from './VueJsonEditor.vue';

export default {
  components: {
    'vue-modal': VueModal,
    'vue-json-editor': VueJsonEditor,
  },
  data: function () {
    return {
      topologies: [],
      form: {
        name: '',
        type: '',
        description: '',
        env: '',
        config: {},
      },
      selected: {},
      openCreateModal: false,
      environments: [],
      versions: [],
    }
  },
  created() {
    this.load()
  },
  watch: {
    '$route': 'load'
  },
  methods: {
    load() {
      let that = this;
      topologyApiV1.fetchAllTopologies().then(data => {
        data.map(topology => topology.disabled = true);
        that.topologies = data
      });
      environmentApiV1.getAllEnvironments().then(response => that.environments = response.data);
    },

    configure(topology) {
      this.form.version = topology.version;
      this.form.type = topology.type;
      this.form.config = topology.config;
      this.form.description = topology.description;
      this.toggleModal();
    },

    deploy() {
      streamsApiV1.createInstance(this.form).then(response => this.goto(response.data.id));
      this.toggleModal();
    },

    goto(id) {
      this.$router.push({path: `/applications/${id}`});
    },

    toggleModal() {
      this.openCreateModal = !this.openCreateModal;
    },

    loadTopologyVersion(event, topology) {
      topology.version = event.target.value;
      topologyApiV1.getTopologyByTypeAndVersion(topology.type, topology.version).then(response => {
        topology.config = response.data.config;
        topology.description = response.data.description;
        topology.disabled = false;
      });
    },

    disableModalDeployBtn() {
      return this.form.name === '';
    },
  }
}
</script>


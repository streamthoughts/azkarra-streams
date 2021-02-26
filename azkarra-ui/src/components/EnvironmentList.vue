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
  <div id="component-environment-list-container">
    <div class="main-content-header">
      <ul class="list-inline">
        <li class="list-inline-item">
          <h1 class="main-title">Execution Environments</h1>
        </li>
        <li class="list-inline-item">
          <div class="dropdown action-btn-right">
            <button class="btn btn-dark dropdown-toggle"
                    type="button"
                    id="dropdownMenuButton" data-toggle="dropdown"
                    aria-haspopup="true" aria-expanded="false">
              Available Actions
            </button>
            <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
              <a class="dropdown-item" href="#" v-on:click="toggleModal()">
                <i class="fas fa-plus"></i> Create Environment
              </a>
            </div>
          </div>
        </li>
      </ul>
    </div>
    <div class="main-content-body container-fluid">
      <div class="row">
        <div class="col">
          <div class="panel border bg-white rounded box-shadow">
            <table class="table">
              <thead>
              <tr>
                <th></th>
                <th>Name</th>
                <th>Type</th>
                <th>Default</th>
                <th>Applications</th>
              </tr>
              </thead>
              <tbody>
              <template v-for="env in orderedEnvironments" :key="env.name">
                <tr>
                  <td></td>
                  <td>
                    <router-link :to="{path: '/environments/' + env.name}">{{ env.name }}</router-link>
                  </td>
                  <td>{{ env.type }}</td>
                  <td>{{ env.is_default }}</td>
                  <td>{{ env.applications.length }}</td>
                </tr>
              </template>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <vue-modal v-if="openCreateModal">
        <template v-slot:header>
          <h3>Create new environments</h3>
        </template>
        <template v-slot:body>
          <form>
            <div class="form-group mb-3">
              <label for="envName">Name</label>
              <input v-model="form.name" type="text" class="form-control" id="envName" aria-describedby="envNameHelp">
              <small id="envNameHelp" class="form-text text-muted">The name to identify this environment</small>
            </div>
            <div class="form-group mb-3">
              <label for="env-type">Type</label>
              <select v-model="form.type" class="custom-select" id="env-type" aria-describedby="envTypeHelp">>
                <template v-for="type in environmentTypes">
                  <option v-bind:value="type">{{ type }}</option>
                </template>
              </select>
              <small id="envTypeHelp" class="form-text text-muted">The type of this environment</small>
            </div>
            <div class="form-group mb-3">
              <label for="envConfigHelp">Configuration</label>
              <div aria-describedby="envConfigHelp">
                <vue-json-editor
                    v-model="form.config"
                    @json-change="onJsonChange"
                    @has-error="onJsonError">
                </vue-json-editor>
              </div>
              <small id="envConfigHelp" class="form-text text-muted">The JSON configuration for this environment</small>
            </div>
          </form>
        </template>
        <template v-slot:footer>
          <button class="btn btn-primary" v-bind:disabled="disableCreateBtn" v-on:click="create">Create</button>
          <button class="btn btn-dark" v-on:click="toggleModal()">Close</button>
        </template>
      </vue-modal>
    </div>
  </div>
</template>

<script>
import environmentApiV1 from '../services/environment.api.js';
import VueJsonPretty from 'vue-json-pretty';
import VueJsonEditor from './VueJsonEditor.vue'
import VueModal from './VueModal.vue';

export default {
  components: {
    'vue-modal': VueModal,
    'vue-json-pretty': VueJsonPretty,
    'vue-json-editor': VueJsonEditor,
  },
  data: function () {
    return {
      options: {
        "mode": "code",
        "search": false,
        "indentation": 2,
        "verbose": false,
        "mainMenuBar": false,
      },
      form: {
        config: {
          streams: {}
        }
      },
      environments: [],
      environmentTypes: [],
      openCreateModal: false,
      disableCreateBtn: false,

    }
  },
  created() {
    this.load();
  },
  watch: {
    '$route': 'load'
  },
  methods: {
    toggleModal() {
      this.openCreateModal = !this.openCreateModal;
      this.form.config = {streams: {}};
      this.form.name = '';
    },

    create() {
      let that = this;
      environmentApiV1.createEnvironment(this.form).then(function () {
        that.load();
      }).finally(function () {
        that.toggleModal();
      });
    },

    load() {
      let that = this;
      environmentApiV1.getAllEnvironments().then(response =>
          that.environments = response.data
      );
      environmentApiV1.getAllEnvironmentsTypes().then(response =>
          that.environmentTypes = response.data
      );
    },
    onJsonChange(json) {
      this.disableCreateBtn = false;
    },

    onJsonError() {
      this.disableCreateBtn = true;
    },
  },
  computed: {
    orderedEnvironments: function () {
      return _.orderBy(this.environments, 'name')
    }
  }
}
</script>


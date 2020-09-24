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
    <div id="environments-list" class="container-fluid">
        <div class="row">
            <div class="col">
                <div class="row justify-content-between">
                    <div class="col"><h2 class="mb-6">Context</h2></div>
                    <div class="col-3 justify-content-end">
                        <div class="dropdown action-btn-right">
                          <button class="btn btn-dark dropdown-toggle"
                            type="button"
                            id="dropdownMenuButton" data-toggle="dropdown"
                            aria-haspopup="true" aria-expanded="false">
                            Available Actions
                          </button>
                          <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
                            <a class="dropdown-item" href="#" v-on:click="toggleModal()">
                                <i class="fas fa-plus"></i></i>Create
                            </a>
                          </div>
                        </div>
                    </div>
                </div>
                <div class="my-3 p-3 bg-white rounded box-shadow">
                    <div class="row">
                        <div class="col-8">
                            <h6 class="border-bottom border-gray pb-2 mb-3">Configuration</h6>
                            <vue-json-pretty
                              :data="context.config"
                              @click="handleClick">
                            </vue-json-pretty>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="row">
            <div class="col">
                <div class="row justify-content-between">
                    <div class="col"><h2 class="mb-6">Environments</h2></div>
                </div>
                <template v-for="env in environments">
                    <div class="my-3 p-3 bg-white rounded box-shadow">
                        <h4 class="border-bottom border-gray pb-2 mb-3">{{ env.name }}</h4>
                        <div class="row">
                            <div class="col-8">
                                <h6 class="border-bottom border-gray pb-2 mb-3">Configuration</h6>
                                <vue-json-pretty
                                  :data="env.config"
                                  @click="handleClick">
                                </vue-json-pretty>
                            </div>
                            <div class="col-4">
                                <h6 class="border-bottom border-gray pb-2 mb-3">Applications
                                <span class="badge badge-dark badge-pill">{{  env.applications.length }}</span>
                                </h6>
                                <ul class="list-group list-group-no-border">
                                     <template v-for="app in env.applications">
                                        <button type="button" class="list-group-item list-group-item-action"
                                            v-on:click="goto($event, app)">
                                            {{ app }}
                                        </button>
                                     </template>
                                </ul>
                            </div>
                        </div>
                    </div>
                </template>
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
                        <label for="envConfig">Configuration</label>
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
</template>

<script>
import azkarra from '../services/azkarra-api.js';
import VueJsonPretty from 'vue-json-pretty';
import VueJsonEditor from './VueJsonEditor.vue'
import VueModal from './VueModal.vue';

export default {
  components: {
    'vue-modal' : VueModal,
    'vue-json-pretty' : VueJsonPretty,
    'vue-json-editor': VueJsonEditor,
  },
  data: function () {
    return {
      options : {
        "mode": "code",
        "search": false,
        "indentation": 2,
        "verbose": false,
        "mainMenuBar" : false,
      },
      form : {
        config : {
          streams: {

          }
        }
      },
      environments: [],
      context: {},
      openCreateModal : false,
      disableCreateBtn : false,

    }
  },
  created () {
     this.load();
  },
  watch: {
    '$route': 'load'
  },
  methods: {
    toggleModal() {
      this.openCreateModal = ! this.openCreateModal;
      this.form.config = { streams:{} };
      this.form.name = '';
    },

    create() {
      let that = this;
      azkarra.createEnvironment(this.form).then(function(){
        that.load();
      }).finally(function () {
        that.toggleModal();
      });
    },

    load() {
      let that = this;
      azkarra.fetchEnvironments().then(function(data){ that.environments = data;});
      azkarra.fetchContext().then(function(data){ that.context = data;});
    },

    goto(app) {
        this.$router.push({ path: `/streams/${app}` });
    },

    onJsonChange(json) {
      this.disableCreateBtn = false;
    },

    onJsonError() {
      this.disableCreateBtn = true;
    },

    goto(event, app) {
      this.$router.push({ path: `/streams/${app}` });
    },
  }
}
</script>

<style scoped>
  .svg-inline--fa {
      height: 0.9em;
      padding-right: 5px;
  }
</style>


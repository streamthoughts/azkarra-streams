/*
 * Copyright 2019 StreamThoughts.
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
  <div id="local-streams-details" class="container-fluid">
     <div class="row justify-content-between">
        <div class="col">
           <ul class="list-inline left-border" v-if="streams.state">
              <li class="list-inline-item">
                <span v-bind:class="getClassForHealthStatus">
                    <i aria-hidden="true" class="fa fa-circle"></i>
                </span>
                {{ streams.name }}
              </li>
              <li class="list-inline-item">{{ streams.since }}</li>
              <li class="list-inline-item">
                {{ streams.state.state }} ({{ streams.state.since }})
              </li>
           </ul>
        </div>
        <div class="col-3 justify-content-end">
            <div class="dropdown action-btn-right">
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
                    <i class="fas fa-trash-alt"></i>Delete
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
              <div class="tab-pane fade show active" id="nav-streams-overview" role="tabpanel" aria-labelledby="nav-streams-overview-tab">
                 <h6 class="tab-pane-title">Streams</h6>
                 <div class="tab-pane-content bg-white rounded box-shadow bordered">
                    <table class="table table-borderless">
                       <tbody>
                          <tr>
                            <td>ID</td>
                            <td>{{ streams.id }}</td>
                          </tr>
                          <tr>
                            <td>Description</td>
                            <td>{{ streams.description }}</td>
                          </tr>
                          <tr>
                            <td>Version</td>
                            <td>{{ streams.version }}</td>
                          </tr>
                      </tbody>
                    </table>
                 </div>
                 <h6 class="tab-pane-title">Instances</h6>
                 <div class="tab-pane-content bg-white rounded box-shadow bordered">
                    <table class="table">
                      <thead>
                        <tr>
                          <th scope="col">Server</th>
                          <th scope="col">Stores</th>
                          <th scope="col">Assignments</th>
                        </tr>
                      </thead>
                      <tbody>
                        <span class="text-no-data" v-if="instances.length == 0">No information available</span>
                        <template v-for="instance in instances">
                          <tr>
                            <td>{{ instance.server }}</td>
                            <td>{{ instance.stores }}</td>
                            <td>
                              <vue-json-pretty
                                :data="instance.assignments"
                                @click="handleClick">
                              </vue-json-pretty>
                            </td>
                         </template>
                      </tbody>
                    </table>
                </div>
              </div>
              <div class="tab-pane fade" id="nav-streams-dag" role="tabpanel" aria-labelledby="nav-streams-dag-tab">
                 <div class="tab-pane-content pl-0 pr-0">
                    <streams-topology v-bind:id="id"></streams-topology>
                 </div>
              </div>
              <div class="tab-pane fade" id="nav-streams-tasks" role="tabpanel" aria-labelledby="nav-streams-tasks-tab">
                <span class="text-no-data" v-if="status.threads == 0">No information available</span>
                <template v-for="thread in status.threads">
                  <div class="mb-3">
                    <div class="tab-pane-title mb-1">
                      <i v-if="thread.state == 'FAILED'" class="fa fa-circle text-danger" aria-hidden="true"></i>
                      <i v-else-if="thread.state == 'RUNNING'" class="fa fa-circle text-success" aria-hidden="true"></i>
                      <i v-else class="fa fa-circle text-warning" aria-hidden="true"></i>
                      StreamThread({{ thread.name }} ({{ thread.state }})
                    </div>
                    <div class="tab-pane-content bg-white rounded box-shadow bordered">
                      <span class="tab-content-subtitle">ActiveTasks</span>
                      <table class="table">
                        <thead>
                          <tr>
                            <th scope="col">TaskID</th>
                            <th scope="col">Topics/Partitions</th>
                            </tr>
                        </thead>
                        <tbody>
                          <template v-for="thread in thread.active_tasks">
                            <tr>
                              <td>{{ thread.task_id }}</td>
                              <td>{{ thread.topic_partitions }}</td>
                            <tr>
                          </template>
                        </tbody>
                      </table>
                      <span class="tab-content-subtitle">StandbyTasks</span>
                      <table class="table">
                        <thead>
                          <tr>
                            <th scope="col">TaskID</th>
                            <th scope="col">Topics/Partitions</th>
                          </tr>
                        </thead>
                        <tbody>
                          <span class="text-no-data" v-if="thread.standby_tasks.length == 0">No standby task</span>
                          <template v-for="thread in thread.standby_tasks">
                            <tr>
                              <td>{{ thread.task_id }}</td>
                              <td>{{ thread.topic_partitions }}</td>
                            <tr>
                          </template>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </template>
              </div>
              <div class="tab-pane fade" id="nav-streams-config" role="tabpanel" aria-labelledby="nav-streams-config-tab">
                <div class="tab-pane-content bg-white rounded box-shadow bordered">
                    <streams-config v-bind:id="id"></streams-config>
                </div>
              </div>
              <div class="tab-pane fade" id="nav-streams-metrics" role="tabpanel" aria-labelledby="nav-streams-metrics-tab">
                <div class="tab-pane-content bg-white rounded box-shadow bordered">
                    <streams-metrics v-bind:id="id"></streams-metrics>
                </div>
              </div>
              <div class="tab-pane fade" id="nav-streams-error-trace" role="tabpanel" aria-labelledby="nav-streams-metrics-tab">
                <div class="tab-pane-content bg-white rounded box-shadow bordered">
                    {{ streams.exception }}
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
               StateStore directory (StreamsConfig.STATE_DIR_CONFIG) by deleting all data with regard to the application ID.
             </div>
             <input class="form-check-input"
                   type="checkbox" value=""
                   id="stopConfirmCleanup"
                   v-model="formConfirmStop.cleanup">
              <label class="form-check-label" for="stopConfirmCleanup">Enable cleanup</label>
           </div>
           <div class="alert alert-danger" role="alert" v-if="isDeleteAction()">
             This action cannot be undone! The streams application will be stopped and removed from its environment.
             Please also keep in mind that: Deleting an application will delete all local data with regard
             to the application.
           </div>
           <div class="form-group">
             <label for="confirmStreamsApplication">Please type in the id of the application to confirm.</label>
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
        		key="stop-btn">{{ actionToConfirm }}</button>
       </template>
     </vue-modal>
  </div>
</template>

<script>
import azkarra from '../services/azkarra-api.js'
import StreamsTopology from './StreamsTopology.vue'
import StreamsMetrics from './StreamsMetrics.vue'
import StreamsConfig from './StreamsConfig.vue'
import VueModal from './VueModal.vue'
import VueJsonPretty from 'vue-json-pretty';

const ActionDelete = "Delete";
const ActionStop = "Stop";

export default {
  components: {
    'streams-topology': StreamsTopology,
    'streams-metrics': StreamsMetrics,
    'streams-config': StreamsConfig,
    'vue-json-pretty': VueJsonPretty,
    'vue-modal': VueModal,
  },
  data: function () {
    return {
      id: "",
      streams : {},
      instances : [],
      status : {},
      openConfirmModal : false,
      formConfirmStop : {
        cleanup: false,
        id: "",
      },
      actionToConfirm : "",
    }
  },
  created () {
    this.id = this.$route.params.id;
    this.load();
    this.timer = setInterval(this.load, 5000);
  },

  watch: {
    '$route': 'load',
  },

  beforeDestroy () {
    clearInterval(this.timer);
  },

  methods: {
    load() {
      var that = this;
      azkarra.getActiveStreamsById({id: this.id}).then(function(data){
        that.streams = data;
      });
      azkarra.fetchApplicationMetadata({id: this.id}).then(function(data) {
        that.instances = data;
      });
      azkarra.fetchLocalActiveStreamsStatusConfig({id: this.id}).then(function(data) {
        that.status = data;
      });
    },

    restart() {
      azkarra.restartLocalStreamsById({id: this.id});
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
        this.actionToConfirm = ActionDelete;
      }
    },

    isConfirmed() {
      return this.formConfirmStop.id == this.id;
    },

    isStoppable() {
      if (this.streams.state) {
        return this.streams.state.state != 'FAILED' && this.streams.state.state != 'NOT_RUNNING';
       } else  {
        return false;
       }
    },

    confirm() {
      if (this.isConfirmed()) {	
      	  let id = {id: this.id};
	      if (this.isStopAction()) {
	      	azkarra.stopLocalStreamsById(id, this.formConfirmStop.cleanup);
	      }

	      if (this.isDeleteAction()) {
	      	azkarra.deleteLocalStreamsById(id).then(() => {
	      	  this.$router.push({ path: `/streams` })
	      	})
	      }
	      this.closeConfirm();
	  }    
    },

    isDeleteAction() {
      return this.actionToConfirm === ActionDelete;
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
    getClassForHealthStatus: function () {
      if (this.streams.state) {
        let state = this.streams.state.state;
        return state == 'RUNNING' ? 'text-success' : state == 'ERROR' ? 'text-danger' : 'text-warning';
      } else {
        return 'text-warning';
      }
    }
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
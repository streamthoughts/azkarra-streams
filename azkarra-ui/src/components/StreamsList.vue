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
    <div id="local-streams-summary" class="container-fluid">
        <div id="local-active-streams" class="row">
            <div class="col">
                <h2>Local Active Streams</h2>
                <div id="summary-active-streams">
                    <table id="summary-active-streams-table" class="table table-striped table-dark">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>ID</th>
                                <th>Version</th>
                                <th>Description</th>
                                <th>Started Since</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                        <template v-for="s in streams">
                            <tr>
                                <td>{{ s.name }}</td>
                                <td>{{ s.id }}</td>
                                <td>{{ s.version }}</td>
                                <td>{{ s.description }}</td>
                                <td>{{ s.sinceHowLong }}</td>
                                <td>
                                    <i v-if="s.state.state == 'ERROR'" class="fa fa-circle text-danger" aria-hidden="true"></i>
                                    <i v-else-if="s.state.state == 'RUNNING'" class="fa fa-circle text-success" aria-hidden="true"></i>
                                    <i v-else class="fa fa-circle text-warning" aria-hidden="true"></i>
                                    {{ s.state.state }}
                                </td>
                                <td>
                                    <div class="btn-group" role="group" aria-label="operations">
                                        <button type="button" class="btn btn-secondary"
                                            v-on:click="goto($event, s.id)"><i class="far fa-eye"></i></button>
                                    </div>
                                </td>
                            </tr>
                        </template>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import azkarra from '../services/azkarra-api.js'

export default {
  data: function () {
    return {
      streams: []
    }
  },
  created () {
     this.load()
  },
  watch: {
    '$route': 'load'
  },
  methods: {
    load() {
      var that = this;
      azkarra.fetchLocalActiveStreams().then(function(data){
        that.streams = data;
      });
    },

    goto(event, id) {
        this.$router.push({ path: `/streams/${id}` })
    },
  },
}
</script>


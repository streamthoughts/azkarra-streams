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
  <div>
    <div class="container-fluid" id="local-streams-topology">
      <div class="row">
        <div class="col">
          <topology-dag v-bind:topology="topology"></topology-dag>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import azkarra from '../services/azkarra-api.js'
import TopologyDAG from './TopologyDAG.vue'

export default {
  components: {
    'topology-dag': TopologyDAG,
  },
  props: ['id'],
  data: function () {
    return {
      topology: {}
    }
  },
  created () {
     this.load()
  },
  watch: {
    'id': 'build',
  },

  methods: {
    load() {
      let that = this;
        azkarra
          .fetchStreamsTopologyById({id: this.id })
          .then(function(data){ that.topology = data; });
    },
  },
}
</script>


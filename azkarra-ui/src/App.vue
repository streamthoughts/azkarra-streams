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
    <div id="app">
      <nav class="navbar navbar-dark fixed-top bg-dark flex-md-nowrap p-0 shadow">
        <a class="navbar-brand col-sm-3 col-md-2 mr-0" href="#">
          <a class="navbar-brand" href="#">
             <img height="62px" alt="azkarra-streams-logo" src="static/azkarra-logo-reverse-small.png"/>
             Dashboard
           </a>
        </a>
        <ul class="navbar-nav text-light px-3">
            <li class="nav-item text-nowrap">
              {{ version.azkarraVersion }} ({{ version.branch }} / {{ version.commitId }})
            </li>
          </ul>
      </nav>
      <div class="main-container">
        <nav class="bg-dark main-sidebar">
          <div class="sidebar-sticky">
            <ul class="nav flex-column">
              <li class="nav-item">
                <router-link to="/" exact class="nav-link">
                  <span class="icon"><i class="fas fa-tachometer-alt"></i></span>
                  <div class="nav-label">Overview</div>
                </router-link>
              </li>
              <li class="nav-item">
                <router-link to="/streams" class="nav-link">
                  <span class="icon"><i class="fas fa-server"></i></span>
                  <div class="nav-label">Active Streams</div>
                </router-link>
              </li>
              <li class="nav-item">
                <router-link to="/topologies" class="nav-link">
                  <span class="icon"><i class="fas fa-project-diagram"></i></span>
                  <div class="nav-label">Topologies</div>
                </router-link>
              </li>
              <li class="nav-item">
                <router-link to="/query" class="nav-link">
                  <span class="icon"><i class="fas fa-search"></i></span>
                  <div class="nav-label">Interactive Queries</div>
                </router-link>
              </li>
              <li class="nav-item">
                <router-link to="/environments" class="nav-link">
                  <span class="icon"><i class="far fa-plus-square"></i></span>
                  <div class="nav-label">Environments</div>
                  </router-link>
              </li>
            </ul>
          </div>
        </nav>
        <main role="main" class="main-content">
          <div class="container-fluid">
            <template v-for="error in errors" :key="error.index">
              <transition name="smooth-slide" mode="out-in">
                  <div class="row">
                    <div class="col alert alert-warning">
                      <a v-on:click="removeAlert(alert)" href="#"
                          class="close"
                          data-dismiss="alert"
                          aria-label="close">&times;</a>
                      <strong>Warning!</strong> {{ error.message }}
                    </div>
                  </div>
               </transition>
            </template>
          </div>
          <transition name="smooth-slide" mode="out-in">
             <router-view></router-view>
          </transition>
        </main>
      </div>
    </div>
</template>

<script>

import azkarra from './services/azkarra-api.js'

export default {
  name: 'app',
  data() {
    return {
      version: {},
      errors: [],
      timers: [],
    }
  },
  created () {
    this.load();
  },
  watch: {
    '$route': 'removeErrors'
  },
  beforeDestroy () {
    this.timers.forEach((timer) => {
      clearInterval(timer);
    });
  },
  methods: {
    load() {
      let that = this;
      azkarra.axios().interceptors.response.use(function (response) {
          return response;
        }, function (error) {
          let alert = (error.response.data == '')
            ? { error_code: error.response.status, message : error.message }
            : error.response.data

          let index = that.errors.length;
          alert.index = index;
          that.errors.push(alert);
          if (length > 3) {
            that.errors.shift();
          }
          let timer = setInterval(that.removeError, 8000, alert);
          that.timers.push(timer);
          return Promise.reject(error.response);
      });
      azkarra.getVersion().then(function(data){ that.version = data });
    },

    removeError(error) {
     this.errors.splice(error.index, 1);
    },

    removeErrors() {
      this.errors = [];
      this.timers.forEach(timer => { clearInterval(timer) });
    }
  }
}
</script>

<style scoped>
  .alert {
    z-index: 1000;
    opacity: 0.95;
    transition: opacity .4s ease;
  }
</style>
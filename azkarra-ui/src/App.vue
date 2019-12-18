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
             <img height="62px" alt="azkarra-streams-logo" src="static/azkarra-streams-logo-light.svg"/>
             <span class="navbar-api-info" v-if="api.headless">(mode : headless)</span>
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
            <template v-for="error in errors" :key="error.id">
              <transition name="smooth-slide" mode="out-in">
                  <div class="row">
                    <div class="col alert alert-warning">
                      <a v-on:click="removeError(error)" href="#"
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
      <div class="login-modal">
      <vue-modal v-if="openAuthModal">
        <template v-slot:header>
           <h3>Authentication</h3>
        </template>
        <template v-slot:body>
          <form>
            <div class="form-row justify-content-md-center">
              <div class="col-md-8 mb-3">
                <div class="alert alert-danger" role="alert" v-if="isAuthAttempted">Invalid username/password</div>
                <div class="input-group mb-2 mr-sm-2">
                  <div class="input-group-prepend">
                    <div class="input-group-text"><i class="fa fa-user"></i></div>
                  </div>
                  <input v-model="auth.username"
                         type="text" class="form-control" id="user" placeholder="username">
                </div>
              </div>
            </div>
            <div class="form-row justify-content-md-center">
              <div class="col-md-8 mb-3">
                <div class="input-group mb-2 mr-sm-2">
                  <div class="input-group-prepend">
                    <div class="input-group-text"><i class="fa fa-lock"></i></div>
                  </div>
                  <input v-model="auth.password"
                         type="password" class="form-control" id="password" placeholder="password">
                </div>
              </div>
            </div>
          </form>
        </template>
        <template v-slot:footer>
          <button class="btn btn-dark" v-on:click="authenticate">Login</button>
        </template>
      </vue-modal>
      </div>
    </div>
</template>

<script>

import azkarra from './services/azkarra-api.js'
import VueModal from './components/VueModal.vue'

var errorsCount = 0;

export default {
  components: {
    'vue-modal': VueModal,
  },
  name: 'app',
  data() {
    return {
      version: {},
      errors: [],
      timers: [],
      openAuthModal: false,
      auth: {},
      isAuthAttempted: false,
      api: { },
    }
  },
  created () {
    let that = this;
    azkarra.axios().interceptors.response.use(
      function (response) {
        return response;
      },
      function (error) {
        let status = error.response.status
        // auth modal should not be opened if server is in headless mode.
        if ( (status == 403 || status == 401) && !that.isHeadless()) {
          that.openAuthModal = true
        } else {
          let errorMessage = (error.response.data == '')
            ? { error_code: error.response.status, message : error.message }
            : error.response.data
          that.pushError(errorMessage);
        }
        return Promise.reject(error.response);
      }
    );
    that.load();
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

    isHeadless() {
      return this.api.headless || false;
    },

    authenticate() {
      let that = this;
      azkarra.setClientAuth(this.auth);

      // trigger request to check authentication
      azkarra.getApi().then(function(d) {
        that.openAuthModal = false
        that.load();
        if (that.$route.path !== "/") {
          that.$router.push({ path: '/' });
        }
      }, function(error) {
        that.isAuthAttempted = true;
      });
    },

    load() {
      let that = this;
      azkarra.getVersion().then(function(data){ that.version = data });
      azkarra.getApi().then(function(data) { that.api = data });
    },

    pushError(error) {
      let that = this;
      let timer = setInterval(that.removeError, 8000, error);
      error.timer = timer;
      error.id = errorsCount++;
      error.index = that.errors.length;
      that.errors.push(error);
      that.timers.push(timer);
      if (that.errors.length > 3) {
        that.removeError(that.errors[0]);
      }
    },

    removeError(error) {
     console.log(error);
     this.errors.splice(error.index, 1);
     this.errors.forEach(function(err, index) { err.index = index });
     clearInterval(error.timer);
    },

    removeErrors() {
      this.timers.forEach(timer => { clearInterval(timer) });
      this.errors = [];
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

  span.navbar-api-info {
    font-size : 0.8em;
  }
</style>

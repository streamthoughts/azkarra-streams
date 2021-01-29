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
              <router-link to="/applications" class="nav-link">
                <span class="icon"><i class="fas fa-rocket"></i></span>
                <div class="nav-label">Applications</div>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/streams" class="nav-link">
                <span class="icon"><i class="fas fa-server"></i></span>
                <div class="nav-label">Instances</div>
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
              <router-link to="/configuration" class="nav-link">
                <span class="icon"><i class="fas fa-cog"></i></span>
                <div class="nav-label">Configuration</div>
              </router-link>
            </li>
            <li class="nav-item">
              <router-link to="/environments" class="nav-link">
                <span class="icon"><i class="far fa-plus-square"></i></span>
                <div class="nav-label">Environments</div>
              </router-link>
            </li>
            <li class="nav-item position-absolute fixed-bottom border-top">
              <router-link to="/about" class="nav-link">
                <span class="icon"><i class="fas fa-info-circle"></i></span>
                <div class="nav-label">About</div>
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
    <notifications group="default"
                   position="bottom right"
                   speed="500"
                   max="6"
                   reverse="true"
                   width="36%"/>
  </div>
</template>

<script>

import httpClient from './services/httpClient.js'
import azkarraApi from './services/azkarra.api.js'
import VueModal from './components/VueModal.vue'

let errorsCount = 0;

export default {
  components: {
    'vue-modal': VueModal,
  },
  name: 'app',
  data() {
    return {
      openAuthModal: false,
      auth: {},
      isAuthAttempted: false,
      api: {},
    }
  },
  created: function () {
    let that = this;
    let notificationId = 0;
    let errorInterceptor = error => {
      if (!error.response) {
        that.$notify({
          group: 'default',
          title: `Network Error (#${notificationId++})`,
          text: `Connection refused (request: ${error.config.method.toUpperCase()}/ ${error.config.url}')`,
          type: 'error'
        });
        return Promise.reject(error);
      }
      let response = error.response;
      if (!(error.config.hasOwnProperty('errorInterceptorEnabled') && !error.config.handlerEnabled)) {
        let status = response.status;
        let error = response.data === '' ? {error_code: status, message: error.message} : response.data;
        switch (status) {
          case 401:
          case 403:
            // auth modal should not be opened if server is in headless mode.
            that.openAuthModal = true;
            that.$notify({
              group: 'default',
              title: `${response.statusText} (#${notificationId++})`,
              text: 'Authentication is required',
              type: 'warn'
            });
            break;
          case 404:
            that.$notify({
              group: 'default',
              title: `${response.statusText} (#${notificationId++})`,
              text: error.message,
              type: 'warn'
            });
            break;
          default:
            this.$notify({
              group: 'default',
              title: `${response.statusText} (#${notificationId++})`,
              text: error.message,
              type: 'error'
            });
        }
      }
      return Promise.reject(response);
    }
    let successInterceptor = response => {
      return response;
    }
    httpClient.interceptors.response.use(successInterceptor, errorInterceptor);
    this.load();
  },
  watch: {
    '$route': 'clear'
  },
  beforeDestroy() {
    this.clear();
  },
  methods: {

    isHeadless() {
      return this.api.headless || false;
    },

    authenticate() {
      let that = this;
      httpClient.defaults.auth = this.auth;

      // trigger request to check authentication
      azkarraApi.getApiV1().then(response => {
        that.openAuthModal = false
        that.load();
        if (that.$route.path !== "/") {
          that.$router.push({path: '/'});
        }
      }, function (error) {
        that.isAuthAttempted = true;
      });
    },

    load() {
      let that = this;
      azkarraApi.getApiV1().then(response => that.api = response.data);
    },
    clear() {
      this.$notify({group: 'default', clean: true});
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
  font-size: 0.8em;
}
</style>

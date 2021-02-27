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
  <div id="app" class="app">
    <div class="main-navbar" :class="{ 'active': menuVisible }">
      <div class="navbar-header">
        <img
             alt="azkarra-streams-logo"
             src="static/azkarra-streams-logo-light.svg"
             v-if="menuVisible"
        />
      </div>
      <nav class="navbar-content">
        <ul class="nav-list">
          <li class="nav-list-item">
            <div class="nav-list-item-content">
              <span class="icon">
                <router-link to="/" exact class="nav-link"><i class="fas fa-tachometer-alt"></i></router-link>
              </span>
              <span class="list-item-text">
                <router-link to="/" exact class="nav-link">Overview</router-link>
              </span>
            </div>
          </li>
          <li class="nav-list-item">
            <div class="nav-list-item-content">
              <span class="icon">
                <router-link to="/applications" class="nav-link"><i class="fas fa-th"></i></router-link>
              </span>
              <span class="list-item-text">
                <router-link to="/applications" class="nav-link">Applications</router-link>
              </span>
            </div>
          </li>
          <li class="nav-list-item">
            <div class="nav-list-item-content">
              <span class="icon">
                <router-link to="/streams" class="nav-link"><i class="fas fa-stream"></i></router-link>
              </span>
              <span class="list-item-text">
                <router-link to="/streams" class="nav-link">Instances</router-link>
              </span>
            </div>
          </li>
          <li class="nav-list-item">
            <div class="nav-list-item-content">
              <span class="icon">
                <router-link to="/topologies" class="nav-link"><i class="fas fa-cubes"></i></router-link>
              </span>
              <span class="list-item-text">
                <router-link to="/topologies" class="nav-link">Topologies</router-link>
              </span>
            </div>
          </li>
          <li class="nav-list-item">
            <div class="nav-list-item-content">
              <span class="icon">
                <router-link to="/query" class="nav-link"><i class="fas fa-search"></i></router-link>
              </span>
              <span class="list-item-text">
                <router-link to="/query" class="nav-link">Interactive Queries</router-link>
              </span>
            </div>
          </li>
          <li class="nav-list-item">
            <div class="nav-list-item-content">
              <span class="icon">
                <router-link to="/configuration" class="nav-link"><i class="fas fa-cog"></i></router-link>
              </span>
              <span class="list-item-text">
                <router-link to="/configuration" class="nav-link">Configuration</router-link>
              </span>
            </div>
          </li>
          <li class="nav-list-item">
            <div class="nav-list-item-content">
              <span class="icon">
                <router-link to="/environments" class="nav-link"><i class="far fa-clone"></i></router-link>
              </span>
              <span class="list-item-text">
                <router-link to="/environments" class="nav-link">Environments</router-link>
              </span>
            </div>
          </li>
          <li class="nav-list-item border-top" style="margin-top: auto;">
            <div class="nav-list-item-content">
              <span class="icon">
                <router-link to="/about" class="nav-link"><i class="fas fa-info-circle"></i></router-link>
              </span>
              <span class="list-item-text">
                <router-link to="/about" class="nav-link">About</router-link>
              </span>
            </div>
          </li>
        </ul>
      </nav>
    </div>
    <div class="main-container">
      <header class="main-header box-shadow">
        <div class="button toggle-menu" @click="toggleMenu()" v-if="menuVisible">
          <format-indent-decrease class="icon"></format-indent-decrease>
        </div>
        <div class="button toggle-menu" @click="toggleMenu()" v-if="!menuVisible">
          <format-indent-increase class="icon"></format-indent-increase>
        </div>
        <div>
          <span class="header-link"><a href="https://www.azkarrastreams.io/" target="_blank">documentation</a></span>
          <span class="header-link"><a href="/apidoc" target="_blank">API</a></span>
          <span class="navbar-api-info" v-if="api.headless">(mode : headless)</span>
        </div>
      </header>
      <main role="main" class="main-content">
        <transition name="component-fade" mode="out-in">
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

import FormatIndentDecrease from 'vue-material-design-icons/FormatIndentDecrease.vue';
import FormatIndentIncrease from 'vue-material-design-icons/FormatIndentIncrease.vue';

let errorsCount = 0;

export default {
  components: {
    'vue-modal': VueModal,
    FormatIndentDecrease,
    FormatIndentIncrease,
  },
  name: 'app',
  data() {
    return {
      openAuthModal: false,
      auth: {},
      isAuthAttempted: false,
      api: { headless: false },
      menuVisible: true
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

      if (!(error.config.hasOwnProperty('errorInterceptorEnabled') && !error.config.errorInterceptorEnabled)) {
        let status = response.status;
        let data = response.data;
        let error = _.isUndefined(data) && data === '' ? {error_code: status, message: error.message} : data;
        switch (status) {
          case 401:
          case 403:
            that.$notify({
              group: 'default',
              title: `${response.statusText} (#${notificationId++})`,
              text: 'Authentication is required',
              type: 'warn'
            });
            that.openAuthModal = true;
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
    toggleMenu () {
      this.menuVisible = !this.menuVisible
    },

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

<style lang="scss">
@import  "./assets/main.scss";
</style>
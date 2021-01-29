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
import Vue from 'vue'
import VueRouter from 'vue-router'
import Notifications from 'vue-notification'

import App from './App.vue'
import StatesQuery from './components/StatesQuery.vue'
import TopologyListPage from './components/TopologyList.vue'
import StreamsListPage from './components/StreamsList.vue'
import StreamsDetailsPage from './components/StreamsDetails.vue'
import OverviewPage from './components/Overview.vue'
import EnvironmentListPage from './components/EnvironmentList.vue'
import EnvironmentDetailsPage from './components/EnvironmentDetails.vue'
import ApplicationListPage from './components/ApplicationList.vue'
import ApplicationDetailsPage from './components/ApplicationDetails.vue'
import Configuration from './components/Configuration.vue'
import About from './components/About.vue'

import 'bootstrap'
import 'bootstrap/dist/css/bootstrap.min.css'

import '@fortawesome/fontawesome-free/css/all.css'
import '@fortawesome/fontawesome-free/js/all.js'

import "./assets/main.css"

Vue.use(VueRouter)
//Vue.use(VueNotification)
Vue.use(Notifications)
Vue.config.productionTip = false

const routes = [
  { path: '/', component: OverviewPage },
  { path: '/topologies', component: TopologyListPage },
  { path: '/streams', component: StreamsListPage },
  { path: '/streams/:id', component: StreamsDetailsPage },
  { path: '/environments', component: EnvironmentListPage },
  { path: '/environments/:name', component: EnvironmentDetailsPage },
  { path: '/applications', component: ApplicationListPage },
  { path: '/applications/:id', component: ApplicationDetailsPage },
  { path: '/query', component: StatesQuery },
  { path: '/configuration', component: Configuration },
  { path: '/about', component: About }
]
const router = new VueRouter({ routes })

new Vue({
  router,
  render: h => h(App),
}).$mount('#app')

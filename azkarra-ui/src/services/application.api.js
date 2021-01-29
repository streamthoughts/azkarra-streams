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
import httpClient from './httpClient';

const API_V1 = '/api/v1';
const API_BASE_PATH = API_V1 + '/applications';

class ApplicationApiV1 {

    getAllApplications() {
        return httpClient.get(API_BASE_PATH);
    }

    getApplicationById(id) {
        return httpClient.get(`${API_BASE_PATH}/${id}`);
    }

    queryApplicationStateStore(query) {
        let url = `${API_BASE_PATH}/${query.application}/stores/${query.store}`;
        let data = {set_options: query.options, type: query.type, query: {[query.operation]: query.params}};
        return httpClient.post(url, data, { timeout : query.options['query_timeout_ms'] });
    }

    getQueryStateStoreAsCurl(query) {
        let url = location.protocol + '//' + location.hostname + (location.port ? ':' + location.port : '');
        url = `${url}${API_BASE_PATH}/${query.application}/stores/${query.store}`;
        let data = {set_options: query.options, type: query.type, query: {[query.operation]: query.params}};
        return 'curl -H "Accept: application/json" -H "Content-Type:application/json" -sX POST ' + url + " --data '" + JSON.stringify(data) + "'";
    }

    deleteApplicationById(id) {
        return httpClient.delete(API_BASE_PATH + '/' + id);
    }
}

const applicationApiV1 = new ApplicationApiV1();
export default applicationApiV1;
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
import moment from 'moment';

const API_V1 = '/api/v1';
const API_BASE_PATH = API_V1 + '/streams';

class StreamApiV1 {

    getInstanceById(id) {
        return httpClient.get(API_BASE_PATH + '/' + id);
    }

    createInstance(data) {
        return httpClient.post(API_BASE_PATH, data);
    }

    deleteInstanceById(id) {
        return httpClient.delete(API_BASE_PATH + '/' + id);
    }

    restartInstanceById(id) {
        return httpClient.post(API_BASE_PATH + '/' + id + "/restart");
    }

    stopInstanceById(id, cleanup) {
        return httpClient.post(API_BASE_PATH + '/' + id + "/stop", {cleanup: cleanup});
    }

    getInstanceMetricsById(id) {
        return httpClient.get(API_BASE_PATH + '/' + id + "/metrics");
    }

    getAllInstancesIds() {
        return httpClient.get(API_BASE_PATH);
    }

    getInstanceConfigById(id) {
        return httpClient.get(API_BASE_PATH + '/' + id + "/config");
    }

    getInstanceStatusById(id) {
        return httpClient.get(API_BASE_PATH + '/' + id + "/status");
    }

    getInstanceMetadataById(id) {
        return httpClient.get(API_BASE_PATH + '/' + id + "/metadata");
    }

    getInstanceOffsetsById(id) {
        return httpClient.get(API_BASE_PATH + '/' + id + "/offsets");
    }

    getInstanceTopologyById(id) {
        return httpClient.get(API_BASE_PATH + '/' + id + "/topology");
    }

    getInstanceStoresById(id) {
        return httpClient.get(API_BASE_PATH + '/' + id + "/stores");
    }


    fetchAllInstances() {
        var that = this
        return httpClient.get(API_BASE_PATH).then(response => {
            var streams = []
            response.data.forEach((id) => {
                that.getInstanceById(id).then(response => {
                    var data = response.data;
                    data.configLoaded = false;
                    data.config = [];
                    data.displayConfig = false;
                    data.sinceHowLong = moment(data.since, "YYYY-MM-DD[T]HH:mm:ss.SSS").fromNow();
                    data.state.since = moment(data.state.since, "YYYY-MM-DD[T]HH:mm:ss.SSS").fromNow();
                    streams.push(data)
                });
            });
            return streams;
        });
    }
}

const streamApiV1 = new StreamApiV1();
export default streamApiV1;
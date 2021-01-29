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
const API_BASE_PATH = API_V1 + '/topologies';

class TopologyApiV1 {

    getTopologyByTypeAndVersion(type, version) {
        return httpClient.get(API_BASE_PATH + '/' + type + "/versions/" + version);
    }

    getAllTopologyVersions(type) {
        return httpClient.get(API_BASE_PATH + '/' + type + "/versions");
    }

    fetchAllTopologies() {
        var that = this
        return httpClient.get(API_BASE_PATH).then(resp1 => {
            return Promise.all(
                resp1.data.map(topology => {
                    return that.getAllTopologyVersions(topology.type).then(resp2 => {
                        return {type: topology.type, aliases: topology.aliases, versions: resp2.data}
                    });
                })
            );
        });
    }
}

const topologyApiV1 = new TopologyApiV1();
export default topologyApiV1;
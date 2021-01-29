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
const API_BASE_PATH = API_V1 + '/environments';

class EnvironmentApiV1 {

    getAllEnvironmentsTypes() {
        return httpClient.get(API_V1 + '/environment-types');
    }

    getAllEnvironments() {
        return httpClient.get(API_BASE_PATH);
    }

    getEnvironmentByName(name) {
        return httpClient.get(API_BASE_PATH + "/" + name);
    }

    createEnvironment(data) {
        return httpClient.post(API_BASE_PATH, data);
    }
}

const environmentApiV1 = new EnvironmentApiV1();
export default environmentApiV1;
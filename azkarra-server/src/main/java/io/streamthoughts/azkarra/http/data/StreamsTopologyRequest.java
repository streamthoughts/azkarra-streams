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
package io.streamthoughts.azkarra.http.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

import static io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext.DEFAULT_ENV_NAME;

public class StreamsTopologyRequest {

    private final String name;
    private final String type;
    private final String version;
    private final String description;
    private final String env;

    private final Map<String, Object> config;

    @JsonCreator
    public StreamsTopologyRequest(final @JsonProperty(value = "name", required = true) String name,
                                  final @JsonProperty(value = "type", required = true) String type,
                                  final @JsonProperty(value = "version", required = true) String version,
                                  final @JsonProperty(value = "env", defaultValue = DEFAULT_ENV_NAME) String env,
                                  final @JsonProperty("description") String description,
                                  final @JsonProperty("config") Map<String, Object> config) {
        this.name = name;
        this.type = type;
        this.version = version;
        this.description = description;
        this.env = env;
        this.config = config != null ? config : Collections.emptyMap();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getEnv() {
        return env;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}

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
package io.streamthoughts.azkarra.http.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsMetadata;

import java.util.List;
import java.util.Objects;

public class StreamsApplicationResponse {

    private final String id;

    private final List<KafkaStreamsMetadata> servers;

    @JsonCreator
    public StreamsApplicationResponse(@JsonProperty("id") final String id,
                                      @JsonProperty("servers") final List<KafkaStreamsMetadata> servers) {
        this.id = Objects.requireNonNull(id, "id should not be null");
        this.servers = Objects.requireNonNull(servers, "servers should not be null");
    }

    public String getId() {
        return id;
    }

    public List<KafkaStreamsMetadata> getServers() {
        return servers;
    }
}

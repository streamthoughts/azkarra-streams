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
package io.streamthoughts.azkarra.api.streams;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.model.HasId;
import io.streamthoughts.azkarra.api.util.Endpoint;

/**
 * A {@code KafkaStreamsInstance} regroups information about a Kafka Streams instance
 * running either locally or remotely.
 */
public class KafkaStreamsInstance implements HasId {

    /**
     * The identifier for this instance, i.e. the container id.
     */
    private final String id;

    /**
     * The endpoint for this instance.
     */
    private final Endpoint endpoint;

    /**
     * A flag to indicate if this instance is running locally.
     */
    private final boolean isLocal;

    /**
     * The metadata for this instance.
     */
    private final KafkaStreamsMetadata metadata;

    /**
     * Creates a new {@link KafkaStreamsInstance} instance.
     *
     * @param id            the identifier for this instance, i.e. the container id.
     * @param endpoint      the endpoint for this instance.
     * @param isLocal       a flag to indicate if this instance is running locally.
     * @param metadata      the metadata for this instance.
     */
    @JsonCreator
    public KafkaStreamsInstance(@JsonProperty("id") final String id,
                                @JsonProperty("endpoint") final Endpoint endpoint,
                                @JsonProperty("isLocal") final boolean isLocal,
                                @JsonProperty("metadata") final KafkaStreamsMetadata metadata) {
        this.id = id;
        this.endpoint = endpoint;
        this.isLocal = isLocal;
        this.metadata = metadata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty("id")
    public String id() {
        return id;
    }

    @JsonProperty("endpoint")
    public Endpoint endpoint() {
        return endpoint;
    }

    @JsonProperty("isLocal")
    public boolean isLocal() {
        return isLocal;
    }

    @JsonProperty("metadata")
    public KafkaStreamsMetadata metadata() {
        return metadata;
    }
}

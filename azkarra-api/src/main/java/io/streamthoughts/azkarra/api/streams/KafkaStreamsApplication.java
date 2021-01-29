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

import java.util.List;
import java.util.Objects;

/**
 * A {@code KafkaStreamsApplication} regroups information about {@code KafkaStreams} instances that use
 * the same {@code application.id} ,i.e., all instances that belong to the same Kafka Streams application.
 */
public class KafkaStreamsApplication implements HasId {

    private final String environment;

    private final String id;

    private final List<KafkaStreamsInstance> instances;

    @JsonCreator
    public KafkaStreamsApplication(@JsonProperty("environment") final String environment,
                                   @JsonProperty("id") final String id,
                                   @JsonProperty("instances") final List<KafkaStreamsInstance> instances) {
        this.environment = Objects.requireNonNull(environment, "id should not be null");
        this.id = Objects.requireNonNull(id, "id should not be null");
        this.instances =  Objects.requireNonNull(instances, "instances should not be null");
    }


    @JsonProperty("environment")
    public String environment() {
        return environment;
    }


    @JsonProperty("instances")
    public List<KafkaStreamsInstance> instances() {
        return instances;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty("id")
    public String id() {
        return id;
    }
}

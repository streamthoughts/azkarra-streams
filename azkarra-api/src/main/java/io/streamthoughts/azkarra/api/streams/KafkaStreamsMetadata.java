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
package io.streamthoughts.azkarra.api.streams;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Set;

/**
 * A {@code KafkaStreamsMetadata} regroups information about resources assigned to a stream application instance,
 * i.e., a set of Topic/Partitions and state stores.
 */
public class KafkaStreamsMetadata {

    public static final KafkaStreamsMetadata EMPTY = new KafkaStreamsMetadata(
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet());

    private final Set<String> stateStores;
    private final Set<TopicPartitions> assignments;
    private final Set<String> standbyStateStores;
    private final Set<TopicPartitions> standbyAssignments;


    /**
     * Creates a new {@link KafkaStreamsMetadata} instance.
     */
    public KafkaStreamsMetadata(final Set<String> stateStores,
                                final Set<TopicPartitions> assignments,
                                final Set<String> standbyStateStores,
                                final Set<TopicPartitions> standbyAssignments) {
        this.stateStores = stateStores;
        this.assignments = assignments;
        this.standbyStateStores = standbyStateStores;
        this.standbyAssignments = standbyAssignments;
    }

    /**
     * Gets the set of store names hosted by this instance.
     *
     * @return the set of stores.
     */
    @JsonProperty("stores")
    public Set<String> stateStores() {
        return stateStores;
    }

    /**
     * Gets the set of topic-partitions assigned to this instance.
     *
     * @return the set of {@link TopicPartitions}.
     */
    @JsonProperty("assignments")
    public Set<TopicPartitions> assignments() {
        return assignments;
    }

    /**
     * Gets the set of standby state store names hosted by this instance.
     *
     * @return the set of stores.
     */
    @JsonProperty("standbyStateStores")
    public Set<String> standbyStateStores() {
        return standbyStateStores;
    }

    /**
     * Gets the set of topic-partitions assigned to this instance for standby state stores.
     *
     * @return the set of {@link TopicPartitions}.
     */
    @JsonProperty("standbyAssignments")
    public Set<TopicPartitions> standbyAssignments() {
        return standbyAssignments;
    }
}


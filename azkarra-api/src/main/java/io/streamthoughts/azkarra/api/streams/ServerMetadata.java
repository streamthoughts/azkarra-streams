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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Set;

/**
 * Class which is used to describe a stream application instance.
 */
public class ServerMetadata {

    private final ServerHostInfo hostInfo;

    private final Set<String> stateStores;
    private final Set<TopicPartitions> assignments;
    private final Set<String> standbyStateStores;
    private final Set<TopicPartitions> standbyAssignments;


    /**
     * Creates a new {@link ServerMetadata} instance.
     */
    public ServerMetadata(final ServerHostInfo hostInfo,
                          final Set<String> stateStores,
                          final Set<TopicPartitions> assignments,
                          final Set<String> standbyStateStores,
                          final Set<TopicPartitions> standbyAssignments) {
        this.hostInfo = Objects.requireNonNull(hostInfo, "hostInfo cannot be null");
        this.stateStores = stateStores;
        this.assignments = assignments;
        this.standbyStateStores = standbyStateStores;
        this.standbyAssignments = standbyAssignments;
    }

    /**
     * Gets the stream application id.
     *
     * @return the string id.
     */
    @JsonProperty("id")
    public String id() {
        return hostInfo.id();
    }

    /**
     * Gets the stream application host.
     *
     * @return the string host.
     */
    public String host() {
        return hostInfo.host();
    }

    /**
     * Gets the stream application port.
     *
     * @return the string port.
     */
    public int port() {
        return hostInfo.port();
    }

    /**
     * Checks whether this instance is local.
     *
     * @return the {@code true} if local.
     */
    @JsonIgnore
    public boolean isLocal() {
        return hostInfo.isLocal();
    }

    @JsonProperty("server")
    public String hostAndPort() {
        return hostInfo.hostAndPort();
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

    @JsonIgnore
    public ServerHostInfo hostInfo() {
        return hostInfo;
    }

}

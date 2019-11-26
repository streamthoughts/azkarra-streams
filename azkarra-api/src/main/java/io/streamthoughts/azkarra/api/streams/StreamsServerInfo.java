/*
 * Copyright 2019 StreamThoughts.
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
import java.util.TreeSet;

/**
 * Class which is used to describe a stream application instance.
 */
public class StreamsServerInfo {

    private final String id;
    private final String host;
    private final int port;
    private final boolean isLocal;
    private final Set<String> stores;
    private final Set<TopicPartitions> assignments;

    /**
     * Creates a new {@link StreamsServerInfo} instance.
     *
     * @param host        the host.
     * @param port        the port.
     * @param stores      the set of stores managed by this instance.
     * @param assignments the set of topic-partitions assigned to this instance.
     */
    public StreamsServerInfo(final String applicationId,
                      final String host,
                      final int port,
                      final Set<String> stores,
                      final Set<TopicPartitions> assignments) {
        this(applicationId, host, port, stores, assignments, false);
    }


    /**
     * Creates a new {@link StreamsServerInfo} instance.
     *
     * @param host      the host.
     * @param port      the port.
     * @param stores      the set of stores managed by this instance.
     * @param assignments the set of topic-partitions assigned to this instance.
     * @param isLocal   is storeName local.
     */
    public StreamsServerInfo(final String applicationId,
                      final String host,
                      final int port,
                      final Set<String> stores,
                      Set<TopicPartitions> assignments,
                      final boolean isLocal) {
        Objects.requireNonNull(applicationId, "id cannot be null");
        Objects.requireNonNull(host, "host cannot be null");
        this.id = applicationId;
        this.host = host;
        this.port = port;
        this.stores = new TreeSet<>(stores);
        this.assignments = new TreeSet<>(assignments);
        this.isLocal = isLocal;
    }

    /**
     * Gets the stream application id.
     *
     * @return the string id.
     */
    @JsonProperty("id")
    public String id() {
        return id;
    }

    /**
     * Gets the stream application host.
     *
     * @return the string host.
     */
    public String host() {
        return host;
    }

    /**
     * Gets the stream application port.
     *
     * @return the string port.
     */
    public int port() {
        return port;
    }

    /**
     * Checks whether this instance is local.
     *
     * @return the {@code true} if local.
     */
    @JsonIgnore
    public boolean isLocal() {
        return isLocal;
    }

    @JsonProperty("server")
    public String hostAndPort() {
        return host + ":" + port;
    }

    /**
     * Gets the set of store names hosted by this instance.
     *
     * @return the set of stores.
     */
    @JsonProperty("stores")
    public Set<String> stores() {
        return stores;
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
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StreamsServerInfo)) return false;
        StreamsServerInfo that = (StreamsServerInfo) o;
        return port == that.port &&
                isLocal == that.isLocal &&
                Objects.equals(id, that.id) &&
                Objects.equals(host, that.host) &&
                Objects.equals(stores, that.stores) &&
                Objects.equals(assignments, that.assignments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, host, port, isLocal, stores, assignments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "StreamsServerInfo{" +
                "id='" + id + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", isLocal=" + isLocal +
                ", stores=" + stores +
                ", assignments=" + assignments +
                '}';
    }
}

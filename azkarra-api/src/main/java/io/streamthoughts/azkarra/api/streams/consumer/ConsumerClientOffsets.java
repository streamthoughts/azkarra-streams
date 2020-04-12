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
package io.streamthoughts.azkarra.api.streams.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsumerClientOffsets {

    /**
     * The consumer client-id.
     * @see org.apache.kafka.clients.consumer.ConsumerConfig#CLIENT_ID_CONFIG
     */
    private String clientId;

    /**
     * The stream-thread name.
     */
    private String streamThread;

    /**
     * The consumer topic/partitions positions.
     */
    private Map<TopicPartition, ConsumerLogOffsets> positions;

    /**
     * Creates a new {@link ConsumerClientOffsets} instance.
     */
    ConsumerClientOffsets(final ConsumerThreadKey consumerThreadKey) {
        this(consumerThreadKey.consumerClientId, consumerThreadKey.consumerThread);
    }

    /**
     * Creates a new {@link ConsumerClientOffsets} instance.
     *
     * @param clientId          the current client-id attached to this topic-partition.
     * @param streamThread      the current stream-thread attached to this topic-partition.
     */
    private ConsumerClientOffsets(final String clientId,
                                  final String streamThread) {
        this(clientId, streamThread, Collections.emptySet());
    }

    /**
     * Creates a new {@link ConsumerClientOffsets} instance.
     *
     * @param clientId          the current client-id attached to this topic-partition.
     * @param streamThread      the current stream-thread attached to this topic-partition.
     */
    public ConsumerClientOffsets(final String clientId,
                                 final String streamThread,
                                 final Set<ConsumerLogOffsets> offsets) {
        this.clientId = clientId;
        this.streamThread = streamThread;
        this.positions = offsets.stream().collect(Collectors.toMap(ConsumerLogOffsets::topicPartition, o -> o));
    }

    @JsonProperty("client_id")
    public String clientId() {
        return clientId;
    }

    @JsonProperty("stream_thread")
    public String streamThread() {
        return streamThread;
    }

    @JsonProperty("positions")
    public List<ConsumerLogOffsets> positions() {
        return new ArrayList<>(positions.values());
    }

    public void update(final ConsumerLogOffsets position) {
        this.positions.put(position.topicPartition(), position);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsumerClientOffsets)) return false;
        ConsumerClientOffsets that = (ConsumerClientOffsets) o;
        return Objects.equals(clientId, that.clientId) &&
                Objects.equals(streamThread, that.streamThread) &&
                Objects.equals(positions, that.positions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, streamThread, positions);
    }

    @Override
    public String toString() {
        return "ConsumerClientOffsets{" +
                "clientId='" + clientId + '\'' +
                ", streamThread='" + streamThread + '\'' +
                ", positions=" + positions +
                '}';
    }
}

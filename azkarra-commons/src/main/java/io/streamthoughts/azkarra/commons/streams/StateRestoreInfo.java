/*
 * Copyright 2021 StreamThoughts.
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
package io.streamthoughts.azkarra.commons.streams;

import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Describes the state of a restoration process for state store.
 *
 * @see LoggingStateRestoreListener
 * @see org.apache.kafka.streams.processor.StateRestoreListener
 */
public class StateRestoreInfo {

    private final String state;
    private final Map<TopicPartition, StatePartitionRestoreInfo> partitionToRestore = new ConcurrentHashMap<>();

    /**
     * Creates a new {@link StateRestoreInfo} instance.
     *
     * @param state the name of the state store.
     */
    public StateRestoreInfo(final String state) {
        this.state = Objects.requireNonNull(state, "state should not be null");
    }

    /**
     * Gets the name of the state being restored.
     */
    public String getState() {
        return this.state;
    }

    /**
     * Gets the state of the TopicPartition being restored.
     */
    public List<StatePartitionRestoreInfo> getAllPartitionRestoreInfos() {
        return new ArrayList<>(partitionToRestore.values());
    }

    /**
     * Adds or updates the state for a TopicPartition being restored.
     *
     * @param info  the {@link StatePartitionRestoreInfo}.
     */
    void addTopicPartitionRestoreInfo(final StatePartitionRestoreInfo info) {
        partitionToRestore.put(new TopicPartition(info.getTopic(), info.getPartition()), info);
    }

    /**
     * Gets the state of the restore process for the given TopicPartition.
     *
     * @param topicPartition    the TopicPartition being restored.
     * @return                  the {@link StatePartitionRestoreInfo}.
     */
    StatePartitionRestoreInfo getTopicPartitionRestoreInfo(final TopicPartition topicPartition) {
        return partitionToRestore.get(topicPartition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "StateRestoreInfo{" +
                "state='" + state + '\'' +
                ", topicPartitions=" + partitionToRestore +
                '}';
    }
}

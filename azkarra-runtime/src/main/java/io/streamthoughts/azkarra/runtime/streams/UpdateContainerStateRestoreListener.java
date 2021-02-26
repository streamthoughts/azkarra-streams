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
package io.streamthoughts.azkarra.runtime.streams;

import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainerAware;
import io.streamthoughts.azkarra.api.streams.State;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.streams.processor.StateRestoreListener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link StateRestoreListener} implementation that updates a {@link KafkaStreamsContainer} state.
 */
final class UpdateContainerStateRestoreListener implements StateRestoreListener, KafkaStreamsContainerAware {

    enum RestoreListenerState implements State {
        STATE_RESTORE_START, STATE_RESTORE_IN_PROGRESS, STATE_RESTORE_COMPLETED
    }

    private final Set<TopicPartition> partitionsToRestore = ConcurrentHashMap.newKeySet();

    private LocalKafkaStreamsContainer container;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRestoreStart(final TopicPartition topicPartition,
                               final String storeName,
                               final long startingOffset,
                               final long endingOffset) {

        // Only update the state to STATE_RESTORE_START for the first topic-partition to restore.
        if (partitionsToRestore.isEmpty()) {
            container.setState(RestoreListenerState.STATE_RESTORE_START);
        }

        partitionsToRestore.add(topicPartition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBatchRestored(final TopicPartition topicPartition,
                                final String storeName,
                                final long batchEndOffset,
                                final long numRestored) {

        container.setState(RestoreListenerState.STATE_RESTORE_IN_PROGRESS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRestoreEnd(final TopicPartition topicPartition,
                             final String storeName,
                             final long totalRestored) {

        partitionsToRestore.remove(topicPartition);

        // Only update the state to STATE_RESTORE_COMPLETED once all topic-partitions have been restored.
        if (partitionsToRestore.isEmpty()) {
            container.setState(RestoreListenerState.STATE_RESTORE_COMPLETED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKafkaStreamsContainer(final KafkaStreamsContainer container) {
        this.container = (LocalKafkaStreamsContainer) container;
    }

}
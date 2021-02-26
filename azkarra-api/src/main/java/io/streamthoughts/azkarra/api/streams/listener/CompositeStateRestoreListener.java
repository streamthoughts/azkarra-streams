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
package io.streamthoughts.azkarra.api.streams.listener;

import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainerAware;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.streams.processor.StateRestoreListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A {@link StateRestoreListener} that delegates to one or more {@link StateRestoreListener} in order.
 */
public class CompositeStateRestoreListener implements StateRestoreListener, KafkaStreamsContainerAware {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeStateRestoreListener.class);

    private final Collection<StateRestoreListener> listeners;

    /**
     * Creates a new {@link CompositeStateRestoreListener} instance.
     *
     * @param listeners the list of {@link StateRestoreListener}.
     */
    public CompositeStateRestoreListener(final Collection<StateRestoreListener> listeners) {
        Objects.requireNonNull(listeners, "listeners cannot be null");
        this.listeners = new ArrayList<>(listeners);
    }

    public void addListener(final StateRestoreListener listener) {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRestoreStart(final TopicPartition topicPartition,
                               final String storeName,
                               final long startingOffset,
                               final long endingOffset) {

        for (StateRestoreListener listener : listeners) {
            try {
                listener.onRestoreStart(topicPartition, storeName, startingOffset, endingOffset);
            } catch (final Exception e) {
                LOG.error(String.format(
                    "Unexpected error happens while executing StateRestoreListener with : " +
                    "topicPartition=%s, " +
                    "storeName=%s, " +
                    "startingOffset=%d, " +
                    "endingOffset=%d",
                    topicPartition,
                    storeName,
                    startingOffset,
                    endingOffset),
                    e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBatchRestored(final TopicPartition topicPartition,
                                final String storeName,
                                final long batchEndOffset,
                                final long numRestored) {
        for (StateRestoreListener listener : listeners) {
            try {
                listener.onBatchRestored(topicPartition, storeName, batchEndOffset, numRestored);
            } catch (final Exception e) {
                LOG.error(String.format(
                    "Unexpected error happens while executing StateRestoreListener with : " +
                    "topicPartition=%s, " +
                    "storeName=%s, " +
                    "batchEndOffset=%d, " +
                    "numRestored=%d",
                    topicPartition,
                    storeName,
                    batchEndOffset,
                    numRestored),
                    e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRestoreEnd(final TopicPartition topicPartition,
                             final String storeName,
                             final long totalRestored) {
        for (StateRestoreListener listener : listeners) {
            try {
                listener.onRestoreEnd(topicPartition, storeName, totalRestored);
            } catch (final Exception e) {
                LOG.error(String.format(
                    "Unexpected error happens while executing StateRestoreListener with : " +
                    "topicPartition=%s, " +
                    "storeName=%s, " +
                    "totalRestored=%d",
                    topicPartition,
                    storeName,
                    totalRestored),
                    e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKafkaStreamsContainer(final KafkaStreamsContainer container) {
        for (StateRestoreListener listener : listeners) {
            if (KafkaStreamsContainerAware.class.isAssignableFrom(listener.getClass())) {
                ((KafkaStreamsContainerAware)listener).setKafkaStreamsContainer(container);
            }
        }
    }
}

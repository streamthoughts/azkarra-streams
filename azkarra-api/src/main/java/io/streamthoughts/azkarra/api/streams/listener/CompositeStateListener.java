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
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A {@link KafkaStreams.StateListener} that delegates to one or more {@link KafkaStreams.StateListener} in order.
 */
public class CompositeStateListener implements KafkaStreams.StateListener, KafkaStreamsContainerAware {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeStateListener.class);

    private final Collection<KafkaStreams.StateListener> listeners = new ArrayList<>();

    /**
     * Creates a new {@link CompositeStateListener} instance.
     */
    public CompositeStateListener() {
        this.listeners.addAll(new ArrayList<>());
    }

    /**
     * Creates a new {@link CompositeStateListener} instance.
     *
     * @param delegates the list of {@link CompositeStateListener}.
     */
    public CompositeStateListener(final Collection<KafkaStreams.StateListener> delegates) {
        Objects.requireNonNull(delegates, "'delegates' cannot be null");
        this.listeners.addAll(delegates);
    }

    public CompositeStateListener addListener(final KafkaStreams.StateListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChange(final KafkaStreams.State newState,
                         final KafkaStreams.State oldState) {

        for (KafkaStreams.StateListener delegate : this.listeners) {
            try {
                delegate.onChange(newState, oldState);
            } catch (final Exception e) {
                LOG.error(String.format(
                    "Unexpected error happens while executing StateListener with : newState=%s, oldState=%s",
                    newState,
                    oldState),
                e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKafkaStreamsContainer(final KafkaStreamsContainer container) {
        for (KafkaStreams.StateListener listener : listeners) {
            if (KafkaStreamsContainerAware.class.isAssignableFrom(listener.getClass())) {
                ((KafkaStreamsContainerAware)listener).setKafkaStreamsContainer(container);
            }
        }
    }
}
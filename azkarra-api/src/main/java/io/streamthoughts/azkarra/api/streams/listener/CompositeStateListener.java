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
package io.streamthoughts.azkarra.api.streams.listener;

import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A {@link KafkaStreams.StateListener} that delegates to one or more {@link KafkaStreams.StateListener} in order.
 */
public class CompositeStateListener implements KafkaStreams.StateListener {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeStateListener.class);

    private final Collection<KafkaStreams.StateListener> delegates = new ArrayList<>();

    /**
     * Creates a new {@link CompositeStateListener} instance.
     */
    public CompositeStateListener() {
        this.delegates.addAll(new ArrayList<>());
    }

    /**
     * Creates a new {@link CompositeStateListener} instance.
     *
     * @param delegates the list of {@link CompositeStateListener}.
     */
    public CompositeStateListener(final Collection<KafkaStreams.StateListener> delegates) {
        Objects.requireNonNull(delegates, "'delegates' cannot be null");
        this.delegates.addAll(delegates);
    }

    public CompositeStateListener addListener(final KafkaStreams.StateListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        delegates.add(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChange(final KafkaStreams.State newState,
                         final KafkaStreams.State oldState) {

        for (KafkaStreams.StateListener delegate : this.delegates) {
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
}
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
package io.streamthoughts.azkarra.api.streams.internal;

import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.State;
import org.apache.kafka.streams.TopologyDescription;

import java.util.Objects;

public class InternalStreamsLifeCycleContext implements StreamsLifecycleContext {

    private final KafkaStreamsContainer container;

    /**
     * Creates a new {@link InternalStreamsLifeCycleContext} instance.
     *
     * @param container the {@link KafkaStreamsContainer} instance
     */
    public InternalStreamsLifeCycleContext(final KafkaStreamsContainer container) {
        this.container = Objects.requireNonNull(container, "container cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApplicationId() {
        return container.applicationId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopologyDescription getTopology() {
        return container.topologyDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getStreamConfig() {
        return container.streamsConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public State getState() {
        return container.state().value();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(final State state) {
       container.setState(state);
    }

    public KafkaStreamsContainer container() {
        return container;
    }
}
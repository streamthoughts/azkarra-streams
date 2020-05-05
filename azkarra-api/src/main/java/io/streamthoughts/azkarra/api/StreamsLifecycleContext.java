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
package io.streamthoughts.azkarra.api;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.util.Version;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.TopologyDescription;


/**
 * Context about the current {@link org.apache.kafka.streams.KafkaStreams} instance.
 */
public interface StreamsLifecycleContext {

    /**
     * Gets {@code application.id} of the current streams instance.
     *
     * @return  return {@code application.id} of the current streams instance.
     */
    String applicationId();

    /**
     * Gets the {@link TopologyDescription} of the current streams instance.
     *
     * @return  the {@link TopologyDescription} instance; cannot be {@code null}.
     */
    TopologyDescription topologyDescription();

    /**
     * @return  the user-specified name for the streams.
     */
    String topologyName();

    /**
     * @return  the version of the streams topology.
     */
    Version topologyVersion();

    /**
     * Gets the configuration of the current streams instance.
     *
     * @return  the {@link Conf} instance; cannot be {@code null}.
     */
    Conf streamsConfig();

    /**
     * Gets the state value of the current streams instance.
     *
     * @return  the {@link State}; cannot be {@code null}.
     */
    State streamsState();

    /**
     * Sets the state of the current streams instance.
     *
     * @param state the new {@link State}.
     */
    void setState(final State state);

    /**
     * Register a watcher to be notified of {@link KafkaStreams.State} change event.
     *
     * @param watcher   the {@link KafkaStreamsContainer.StateChangeWatcher} to be registered.
     */
    void addStateChangeWatcher(final KafkaStreamsContainer.StateChangeWatcher watcher);

}

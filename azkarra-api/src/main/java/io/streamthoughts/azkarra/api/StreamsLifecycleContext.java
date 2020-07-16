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
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.api.util.Version;
import org.apache.kafka.streams.TopologyDescription;


/**
 * Context about the current {@link org.apache.kafka.streams.KafkaStreams} instance.
 */
public interface StreamsLifecycleContext {

    /**
     * @see KafkaStreamsContainer#applicationId()
     */
    default String applicationId() {
        return container().applicationId();
    }

    /**
     * @see KafkaStreamsContainer#topologyDescription()
     */
    default TopologyDescription topologyDescription() {
        return container().topologyDescription();
    }

    /**
     * @see TopologyMetadata#name()
     */
    default String topologyName() {
        return container().topologyMetadata().name();
    }

    /**
     * @see TopologyMetadata#version()
     */
    default Version topologyVersion() {
        return Version.parse(container().topologyMetadata().version());
    }

    /**
     * @see KafkaStreamsContainer#streamsConfig()
     */
    default Conf streamsConfig() {
        return container().streamsConfig();
    }

    /**
     * @see KafkaStreamsContainer#state()
     */
    default State streamsState() {
        return container().state().value();
    }

    /**
     * Sets the state of the current streams instance.
     *
     * @param state the new {@link State}.
     */
    void setState(final State state);

    /**
     * @return the {@link KafkaStreamsContainer}; cannot be {@code null}.
     */
    KafkaStreamsContainer container();
}

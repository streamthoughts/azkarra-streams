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
package io.streamthoughts.azkarra.api.streams.topology;

import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyDescription;

import java.util.List;
import java.util.Objects;

/**
 * Default class to encapsulate a {@link Topology} instance.
 */
public class TopologyContainer {

    private TopologyDescription description;

    private final Topology topology;

    private final TopologyMetadata metadata;

    private final ApplicationId applicationId;

    private final List<StreamsLifecycleInterceptor> interceptors;

    private Conf streamsConfig;

    /**
     * Creates a new {@link TopologyContainer} instance.
     *
     * @param topology       the {@link Topology} instance.
     * @param streamsConfig  the {@link Conf} of the streams.
     * @param metadata       the {@link TopologyMetadata} instance.
     *
     */
    public TopologyContainer(final Topology topology,
                             final ApplicationId applicationId,
                             final Conf streamsConfig,
                             final TopologyMetadata metadata,
                             final List<StreamsLifecycleInterceptor> interceptors) {
        this.topology = Objects.requireNonNull(topology, "topology can't be null");
        this.applicationId = Objects.requireNonNull(applicationId, "topology can't be null");
        this.metadata = Objects.requireNonNull(metadata, "metadata can't be null");
        this.streamsConfig = Objects.requireNonNull(streamsConfig, "streamsConfig can't be null");
        this.interceptors = interceptors;
    }

    public List<StreamsLifecycleInterceptor> interceptors() {
        return interceptors;
    }

    public ApplicationId applicationId() {
        return applicationId;
    }

    public Topology topology() {
        return topology;
    }

    public TopologyDescription description() {
        if (description == null) {
            description = topology.describe();
        }
        return description;
    }

    public Conf streamsConfig() {
        return streamsConfig;
    }

    public void streamsConfig(final Conf streamsConfig) {
        this.streamsConfig = streamsConfig;
    }

    public TopologyMetadata metadata() {
        return metadata;
    }
}

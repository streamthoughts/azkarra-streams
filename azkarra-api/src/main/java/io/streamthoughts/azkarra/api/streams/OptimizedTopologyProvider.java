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
package io.streamthoughts.azkarra.api.streams;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

/**
 * A {@link TopologyProvider} that can be used to build an optimized {@link Topology} instance.
 */
public abstract class OptimizedTopologyProvider implements TopologyProvider, Configurable {

    private static final Conf OPTIMIZED_CONF = Conf.of(StreamsConfig.TOPOLOGY_OPTIMIZATION, StreamsConfig.OPTIMIZE);

    private Conf configuration;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns {@link Conf} defined for this {@link TopologyProvider}.
     *
     * @return a {@link Conf} instance.
     */
    protected Conf configuration() {
        checkState();
        return getOptimizedConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Topology get() {
        checkState();
        return getStreamBuilder().build(getOptimizedConfiguration().getConfAsProperties());
    }

    private Conf getOptimizedConfiguration() {
        return configuration.withFallback(OPTIMIZED_CONF);
    }

    private void checkState() {
        if (configuration == null) {
            throw new IllegalStateException(String.format("%s is not configured", getClass().getName()));
        }
    }

    /**
     * Returns the {@link StreamsBuilder} which is used to build the {@link Topology} instance.
     *
     * @return  a {@link StreamsBuilder} instance.
     */
    protected abstract StreamsBuilder getStreamBuilder();
}
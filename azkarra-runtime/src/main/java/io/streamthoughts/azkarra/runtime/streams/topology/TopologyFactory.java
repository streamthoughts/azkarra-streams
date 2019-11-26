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
package io.streamthoughts.azkarra.runtime.streams.topology;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentAware;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.streams.topology.TopologyContainer;
import org.apache.kafka.streams.Topology;

import java.util.Objects;

/**
 * TopologyFactory.
 */
public class TopologyFactory {

    private static final String STREAMS_CONFIG  = "streams";

    private final StreamsExecutionEnvironment environment;

    /**
     * Creates a new {@link TopologyFactory} instance.
     *
     * @param environment  the {@link StreamsExecutionEnvironment} instance.
     */
    public TopologyFactory(final StreamsExecutionEnvironment environment) {
        Objects.requireNonNull(environment, "environment cannot be null");
        this.environment = environment;
    }

    /**
     * Makes a {@link Topology} instance using the specified {@link TopologyDescriptor}.
     *
     * @param provider        the {@link TopologyProvider} instance.
     * @param defaultConf     the {@link Conf} instance to be used for configuring the topology.
     * @param executed        the {@link Executed} instance.
     *
     * @return  a new {@link Topology} instance.
     */
    public TopologyContainer make(final TopologyProvider provider,
                                  final Conf defaultConf,
                                  final Executed executed) {
        Objects.requireNonNull(provider, "provider cannot be empty");
        Objects.requireNonNull(defaultConf, "defaultConf cannot be empty");
        Objects.requireNonNull(executed, "executed cannot be empty");

        final InternalExecuted internal = new InternalExecuted(executed);

        String name = internal.name();
        if (name == null) {
            throw new IllegalStateException("Cannot create a new TopologyContainer with empty name");
        }

        if (provider instanceof StreamsExecutionEnvironmentAware) {
            ((StreamsExecutionEnvironmentAware)provider).setExecutionEnvironment(environment);
        }

        final Conf enrichedConfig = internal.config().withFallback(defaultConf);
        final InternalExecuted enriched = new InternalExecuted(internal.withConfig(enrichedConfig));

        Configurable.mayConfigure(provider, enrichedConfig);

        final Topology topology = provider.get();
        return TopologyContainer.newBuilder()
            .withName(name)
            .withVersion(provider.version())
            .withDescription(enriched.description())
            .withTopology(topology)
            .withConf(getPrefixedWith(STREAMS_CONFIG, enriched.config()))
            .build();
    }

    private Conf getPrefixedWith(final String prefix, Conf configuration) {
        return configuration.hasPath(prefix) ? configuration.getSubConf(prefix) : Conf.empty();
    }
}

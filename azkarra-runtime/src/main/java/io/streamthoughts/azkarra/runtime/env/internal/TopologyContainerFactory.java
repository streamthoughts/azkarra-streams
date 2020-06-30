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
package io.streamthoughts.azkarra.runtime.env.internal;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.streams.topology.TopologyContainer;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * TopologyContainerFactory.
 */
public class TopologyContainerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyContainerFactory.class);

    private static final String STREAMS_CONFIG  = "streams";

    private final StreamsExecutionEnvironment environment;

    private Supplier<ApplicationIdBuilder> applicationIdBuilderSupplier;

    /**
     * Creates a new {@link TopologyContainerFactory} instance.
     *
     * @param environment  the {@link StreamsExecutionEnvironment} instance.
     */
    public TopologyContainerFactory(final StreamsExecutionEnvironment environment,
                                    final Supplier<ApplicationIdBuilder> applicationIdBuilderSupplier) {
        Objects.requireNonNull(environment, "environment cannot be null");
        this.environment = environment;
        this.applicationIdBuilderSupplier = applicationIdBuilderSupplier;
    }

    public void setApplicationIdBuilder(final Supplier<ApplicationIdBuilder> applicationIdBuilderSupplier) {
        this.applicationIdBuilderSupplier = applicationIdBuilderSupplier;
    }

    public Supplier<ApplicationIdBuilder> getApplicationIdBuilderSupplier() {
        return applicationIdBuilderSupplier;
    }

    /**
     * Makes a {@link Topology} instance using the specified {@link TopologyDescriptor}.
     *
     * @param providerSupplier the {@link TopologyProvider} instance.
     * @param executed         the {@link Executed} instance.
     *
     * @return  a new {@link Topology} instance.
     */
    public TopologyContainer make(final Supplier<TopologyProvider> providerSupplier,
                                  final Executed executed) {
        Objects.requireNonNull(providerSupplier, "providerSupplier cannot be empty");
        Objects.requireNonNull(executed, "executed cannot be empty");

        final InternalExecuted internal = new InternalExecuted(executed);

        String name = internal.name();
        if (name == null) {
            throw new IllegalStateException("Cannot create a new TopologyContainer with empty name");
        }

        final Conf topologyConfig = internal.config();

        TopologyProvider provider = supply(providerSupplier, topologyConfig);
        final TopologyMetadata metadata = new TopologyMetadata(name, provider.version(), internal.description());
        LOG.info("Building new Topology for name='{}', version='{}'", name, provider.version());

        List<StreamsLifecycleInterceptor> interceptors = internal.interceptors()
                .stream()
                .map(i -> supply(i, topologyConfig))
                .collect(Collectors.toList());

        interceptors.forEach(i -> LOG.info("Adding streams interceptor: {}", i.name()));

        Conf streamsConfig = streamsConfig(topologyConfig);

        final ApplicationId applicationId = supply(applicationIdBuilderSupplier, topologyConfig)
                .buildApplicationId(metadata, streamsConfig);

        streamsConfig = Conf
                .with(StreamsConfig.APPLICATION_ID_CONFIG, applicationId.toString())
                .withFallback(streamsConfig);

        Topology topology = provider.get();
        return new TopologyContainer(topology, applicationId, streamsConfig, metadata, interceptors);
    }

    private <T> T supply(final Supplier<T> supplier, final Conf topologyConfig) {
        return new EnvironmentAwareComponentSupplier<>(supplier).get(environment, topologyConfig);
    }

    private Conf streamsConfig(final Conf configuration) {
        return configuration.hasPath(TopologyContainerFactory.STREAMS_CONFIG) ?
            configuration.getSubConf(TopologyContainerFactory.STREAMS_CONFIG) :
            Conf.empty();
    }
}

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
package io.streamthoughts.azkarra.streams.context;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import io.streamthoughts.azkarra.streams.context.internal.ApplicationConfig;
import io.streamthoughts.azkarra.streams.context.internal.EnvironmentConfig;
import io.streamthoughts.azkarra.streams.context.internal.TopologyConfig;

import java.util.List;
import java.util.Objects;

/**
 * Class which can be used for initializing an {@link AzkarraContext} instance from a specified {@link Conf} instance.
 *
 * @see ApplicationConfig
 * @see EnvironmentConfig
 * @see TopologyConfig
 */
public class AzkarraContextLoader {

    /**
     * Initialize the specified {@link AzkarraContext} using the specified {@link Conf}.
     *
     * @param context           the {@link AzkarraContext} to initialize.
     * @param configuration     the {@link Conf} instance.
     *
     * @return                  a new {@link AzkarraContext} instance.
     */
    public static AzkarraContext load(final AzkarraContext context, final Conf configuration) {
        return load(context, ApplicationConfig.read(configuration));
    }

    /**
     * Initialize the specified {@link AzkarraContext}  using the specified {@link ApplicationConfig}.
     *
     * @param context           the {@link AzkarraContext} to initialize.
     * @param configuration     the {@link ApplicationConfig} instance.
     *
     * @return                  a new {@link AzkarraContext} instance.
     */
    public static AzkarraContext load(final AzkarraContext context, final ApplicationConfig configuration) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(configuration, "contextConfig cannot be null");

        loadConfiguration(context, configuration);
        loadConfigurationDeclaredProviders(configuration, context);
        loadConfigurationDeclaredEnvironments(configuration, context);

        return context;
    }

    private static void loadConfiguration(final AzkarraContext context,
                                          final ApplicationConfig config) {

        context.addConfiguration(config.context());
    }

    private static void loadConfigurationDeclaredProviders(final ApplicationConfig config,
                                                           final AzkarraContext context) {
        config.components().forEach(context::addComponent);
    }

    private static void loadConfigurationDeclaredEnvironments(final ApplicationConfig config,
                                                              final AzkarraContext context) {

        config.environments().forEach(conf -> initializeStreamsEnvironment(conf, context));
    }

    private static void initializeStreamsEnvironment(final EnvironmentConfig envConfig,
                                                     final AzkarraContext context) {

        final String name = envConfig.name();

        StreamsExecutionEnvironment env;
        if (name == null) {
            env = DefaultStreamsExecutionEnvironment.create(envConfig.config());
        } else {
            env = DefaultStreamsExecutionEnvironment.create(envConfig.config(), name);
        }

        context.addExecutionEnvironment(env);

        List<TopologyConfig> topologyConfigs = envConfig.topologyStreamConfigs();
        for (TopologyConfig topology : topologyConfigs) {

            Executed executed = Executed.as(topology.name().orElse(null));
            executed = topology.config().map(executed::withConfig).orElse(executed);
            executed = topology.description().map(executed::withDescription).orElse(executed);

            context.addTopology(topology.type(), env.name(), executed);
        }
    }
}

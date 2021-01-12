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
package io.streamthoughts.azkarra.api;

import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.components.ConfigurableComponentFactory;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.AlreadyExistsException;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import org.apache.kafka.streams.KafkaStreams;

import java.util.List;
import java.util.Set;

/**
 * The AzkarraContext.
 */
public interface AzkarraContext extends ConfigurableComponentFactory, ComponentRegistry {

    /**
     * Gets the internal {@link ComponentFactory}.
     *
     * @return  the {@link ComponentFactory} instance to be used.
     */
    @Override
    ComponentFactory getComponentFactory();

    /**
     * Registers a new listener instance to this context.
     *
     * @param listener  the {@link AzkarraContextListener} instance to register.
     * @return          this {@link AzkarraContext} instance.
     */
    AzkarraContext addListener(final AzkarraContextListener listener);

    /**
     * Sets if the created {@link AzkarraContext} should have a shutdown hook registered.
     *
     * Defaults to {@code true} to ensure that JVM shutdowns are handled gracefully.
     * @param registerShutdownHook if the shutdown hook should be registered
     *
     * @return          this {@link AzkarraContext} instance.
     */
    AzkarraContext setRegisterShutdownHook(final boolean registerShutdownHook);

    /**
     * Returns the global context streamsConfig of this {@link AzkarraContext} instance.
     *
     * @return a {@link Conf} instance.
     */
    @Override
    Conf getConfiguration();

    /**
     * Sets the default configuration to be used for this {@link AzkarraContext}.
     *
     * @param configuration the {@link Conf} instance.
     *
     * @return          this {@link AzkarraContext} instance.
     */
    AzkarraContext setConfiguration(final Conf configuration);

    /**
     * Adds the specified {@link Conf} to the configuration of this {@link AzkarraContext}.
     *
     * @param configuration the {@link Conf} instance to be used.
     * @return              this {@link AzkarraContext} instance.
     */
    AzkarraContext addConfiguration(final Conf configuration);

    /**
     * Adds the {@link StreamsExecutionEnvironment} to this context.
     *
     * @param environment   the {@link StreamsExecutionEnvironment} instance.
     * @return              this {@link AzkarraContext} instance.
     *
     * @throws AlreadyExistsException   if a {@link StreamsExecutionEnvironment}
     *                                  is already registered for the given name.
     */
    AzkarraContext addExecutionEnvironment(final StreamsExecutionEnvironment<?> environment)
            throws AlreadyExistsException;

    /**
     * Adds a topology to the default environment of this context.
     *
     * @param type          the fully qualified class name or alias of the target {@link TopologyProvider}.
     * @param executed      the {@link Executed} instance.
     *
     * @return              the {@link ApplicationId} instance if the environment is already started,
     *                      otherwise {@code null}.
     */
    ApplicationId addTopology(final String type, final Executed executed);

    /**
     * Adds a topology to the default environment of this context.
     *
     * @param type          the {@link TopologyProvider} class to add.
     * @param executed      the {@link Executed} instance.
     *
     * @return              the {@link ApplicationId} instance if the environment is already started,
     *                      otherwise {@code null}.
     */
    ApplicationId addTopology(final Class<? extends TopologyProvider> type, final Executed executed);

    /**
     * Adds a topology to a specified environment.
     *
     * @param type          the {@link TopologyProvider} class to add.
     * @param environment   the environment name.
     * @param executed      the {@link Executed} instance.
     *
     * @return              the {@link ApplicationId} instance if the environment is already started,
     *                      otherwise {@code null}..
     */
    ApplicationId addTopology(final Class<? extends TopologyProvider> type,
                              final String environment,
                              final Executed executed);

    /**
     * Adds a topology to a specified environment.
     *
     * @param type          the fully qualified class name or alias of the target {@link TopologyProvider}.
     * @param environment   the environment name.
     * @param executed      the {@link Executed} instance.
     *
     * @return              the {@link ApplicationId} instance if the environment is already started,
     *                      otherwise {@code null}.
     */
    ApplicationId addTopology(final String type, final String environment, final Executed executed);

    /**
     * Adds a topology to a specified environment.
     *
     * @param type          the fully qualified class name or alias of the target {@link TopologyProvider}.
     * @param version       the topology version.
     * @param environment   the environment name.
     * @param executed      the {@link Executed} instance.
     *
     * @return              the {@link ApplicationId} instance if the environment is already started,
     *                      otherwise {@code null}.
     */
    ApplicationId addTopology(final String type,
                              final String version,
                              final String environment,
                              final Executed executed);

    /**
     * Gets all topologies registered into this {@link AzkarraContext} even those ones which are not enable.
     *
     * @return a set of {@link TopologyDescriptor}.
     */
    Set<TopologyDescriptor> getTopologyDescriptors();

    /**
     * Gets all topologies registered into this {@link AzkarraContext} which are available for the given environment.
     *
     * @param environmentName the {@link StreamsExecutionEnvironment}
     * @return                a set of {@link TopologyDescriptor}.
     */
    Set<TopologyDescriptor> getTopologyDescriptors(final String environmentName);


    /**
     * Gets all topologies registered into this {@link AzkarraContext} which are available for the given environment.
     *
     * @param env the {@link StreamsExecutionEnvironment}
     * @return    a set of {@link TopologyDescriptor}.
     */
    Set<TopologyDescriptor> getTopologyDescriptors(final StreamsExecutionEnvironment<?> env);

    /**
     * Gets all {@link StreamsExecutionEnvironment} registered to this context.
     *
     * @return  a list of {@link StreamsExecutionEnvironment} instance.
     */
    List<StreamsExecutionEnvironment<?>> getAllEnvironments();

    /**
     * Gets the {@link StreamsExecutionEnvironment} for the specified name.
     *
     * @param envName   the environment name.
     * @return          a {@link StreamsExecutionEnvironment} instance with the specified name.
     */
    StreamsExecutionEnvironment<?> getEnvironmentForName(final String envName);

    /**
     * Gets the default {@link StreamsExecutionEnvironment}.
     * This method can return {@code null} while the context is not started.
     *
     * @return a {@link StreamsExecutionEnvironment} instance.
     */
    StreamsExecutionEnvironment<?> getDefaultEnvironment();

    /**
     * Starts this {@link AzkarraContext} instance.
     */
    void start();

    /**
     * Stops this {@link AzkarraContext} instance.
     *
     * @param cleanUp if local states of each {@link KafkaStreams} instance must be cleanup.
     * @see KafkaStreams#cleanUp() .
     */
    void stop(boolean cleanUp);

    /**
     * Stops this {@link AzkarraContext} instance.
     */
    default void stop() {
        stop(false);
    }
}

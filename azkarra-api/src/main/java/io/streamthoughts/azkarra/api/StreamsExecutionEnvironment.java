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
import io.streamthoughts.azkarra.api.config.RocksDBConfig;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.processor.StateRestoreListener;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * A StreamsExecutionEnvironment manages the lifecycle of {@link org.apache.kafka.streams.Topology} instances.
 */
public interface StreamsExecutionEnvironment {

    String ENABLE_WAIT_FOR_TOPICS_CONFIG = "enable.wait.for.topics";

    /**
     * Gets the name of this {@link StreamsExecutionEnvironment}.
     *
     * @return  the string name.
     */
    String name();

    /**
     * Gets the state f this {@link StreamsExecutionEnvironment}.
     * @return  the {@link State}.
     */
    State state();

    /**
     * Adds a {@link KafkaStreams.StateListener} instance that will set to all {@link KafkaStreams} instance created
     * in this {@link StreamsExecutionEnvironment}.
     *
     * @see KafkaStreams#setStateListener(KafkaStreams.StateListener).
     *
     * @param listener  the {@link KafkaStreams.StateListener} instance.
     *
     * @throws IllegalStateException if this {@link StreamsExecutionEnvironment} instance is started.
     *
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    StreamsExecutionEnvironment addStateListener(final KafkaStreams.StateListener listener);

    /**
     * Adds a {@link StateRestoreListener} instance that will set to all {@link KafkaStreams} instance created
     * in this {@link StreamsExecutionEnvironment}.
     *
     * @see KafkaStreams#setGlobalStateRestoreListener(StateRestoreListener) .
     *
     * @param listener  the {@link StateRestoreListener} instance.
     *
     * @throws IllegalStateException if this {@link StreamsExecutionEnvironment} instance is started.
     *
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    StreamsExecutionEnvironment addGlobalStateListener(final StateRestoreListener listener);

    /**
     * Returns all {@link KafkaStreams} started applications.
     *
     * @return  a collection of {@link KafkaStreamsContainer} applications.
     */
    Collection<KafkaStreamsContainer> applications();

    /**
     * Sets this environment configuration.
     *
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    StreamsExecutionEnvironment setConfiguration(final Conf configuration);

    /**
     * Gets this environment configuration.
     *
     * @return the {@link Conf} instance.
     */
    Conf getConfiguration();

    /**
     * Sets the {@link RocksDBConfig} streamsConfig used by topology persistent stores.
     *
     * @param settings     the {@link RocksDBConfig} instance.
     *
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    StreamsExecutionEnvironment setRocksDBConfig(final RocksDBConfig settings);

    /**
     * Sets the {@link ApplicationIdBuilder} that should be used for building streams {@code application.id}.
     *
     * @param supplier  the {@link ApplicationIdBuilder} instance supplier.
     * @return          this {@link StreamsExecutionEnvironment} instance.
     */
    StreamsExecutionEnvironment setApplicationIdBuilder(final Supplier<ApplicationIdBuilder> supplier);

    /**
     * Sets if the streams instances should wait for topics source to be created before starting.
     * If some source topics are missing at startup, a streams instance fails.
     *
     * @param waitForTopicToBeCreated   should wait for topics to be created.
     * @return                          this {@link StreamsExecutionEnvironment} instance.
     */
    StreamsExecutionEnvironment setWaitForTopicsToBeCreated(final boolean waitForTopicToBeCreated);

    /**
     * Adds streamsConfig that will be used in fallback if not present in defined environment streamsConfig.
     *
     * @param settings  the {@link Conf} instance.
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    StreamsExecutionEnvironment addFallbackConfiguration(final Conf settings);

    /**
     * Add a new {@link TopologyProvider} instance to this {@link StreamsExecutionEnvironment} to be started.
     *
     * @param provider     the {@link TopologyProvider} supplier.
     *
     * @return             this {@link ApplicationId} instance if the environment is already started,
     *                     otherwise {@code null}.
     */
    ApplicationId addTopology(final Supplier<TopologyProvider> provider);

    /**
     * Add a new {@link TopologyProvider} instance to this {@link StreamsExecutionEnvironment} to be started.
     *
     * @param provider     the {@link TopologyProvider} supplier.
     * @param executed     the {@link Executed} instance.
     *
     * @return             this {@link ApplicationId} instance if the environment is already started,
     *                     otherwise {@code null}.
     */
    ApplicationId addTopology(final Supplier<TopologyProvider> provider, final Executed executed);

    /**
     * Starts this {@link StreamsExecutionEnvironment} instance.
     */
    void start();

    /**
     * Stops this {@link StreamsExecutionEnvironment} instance and all running {@link KafkaStreams} instance.
     */
    default void stop() {
        stop(false);
    }

    /**
     * Stops this {@link StreamsExecutionEnvironment} instance and all running {@link KafkaStreams} instance.
     *
     * @param cleanUp if local states of each {@link KafkaStreams} instance must be cleanup.
     * @see KafkaStreams#cleanUp() .
     */
    void stop(boolean cleanUp);

    /**
     * Stops the streams instance for the specified application id.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @param cleanUp if local states of each {@link KafkaStreams} instance must be cleanup.
     * @see KafkaStreams#cleanUp() .
     */
    void stop(final ApplicationId id, boolean cleanUp);

    /**
     * Stops the streams instance for the specified application id and remove the associated topology from this
     * environment.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @see KafkaStreams#cleanUp() .
     */
    void remove(final ApplicationId id);
}

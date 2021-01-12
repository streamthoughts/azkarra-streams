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

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.RocksDBConfig;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import org.apache.kafka.streams.KafkaStreams;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A StreamsExecutionEnvironment manages the lifecycle of {@link org.apache.kafka.streams.Topology} instances.
 */
public interface StreamsExecutionEnvironment<T extends StreamsExecutionEnvironment<T>> {

    /**
     * Gets the type of this {@link StreamsExecutionEnvironment}.
     *
     * @return  the string type.
     */
    String type();

    /**
     * Gets the name of this {@link StreamsExecutionEnvironment}.
     *
     * @return  the string name.
     */
    String name();

    /**
     * Gets the state of this {@link StreamsExecutionEnvironment}.
     * @return  the {@link State}.
     */
    State state();

    /**
     * Check whether this {@link StreamsExecutionEnvironment} is marked as default.
     *
     * @return  {@code true} if this {@link StreamsExecutionEnvironment} is marked as default.
     */
    boolean isDefault();

    /**
     * Creates a new {@link StreamsTopologyExecution} to be applied on this {@link StreamsExecutionEnvironment}.
     *
     * @param meta              the {@link StreamsTopologyMeta} to executed.
     * @param executed          the execution options.
     * @return                  the new {@link StreamsTopologyExecution} instance.
     */
    StreamsTopologyExecution newTopologyExecution(final StreamsTopologyMeta meta, final Executed executed);

    /**
     * Returns all containers for active Kafka Streams applications.
     *
     * @return  a collection of {@link KafkaStreamsContainer} applications.
     */
    Collection<KafkaStreamsContainer> applications();

    /**
     * Returns all ids for active Kafka Streams applications.
     *
     * @return  a collection of {@link KafkaStreamsContainer} applications.
     */
    Set<String> applicationIds();

    /**
     * Sets this environment configuration.
     *
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    T setConfiguration(final Conf configuration);

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
    T setRocksDBConfig(final RocksDBConfig settings);

    /**
     * Sets the {@link ApplicationIdBuilder} that should be used for building streams {@code application.id}.
     *
     * @param supplier  the {@link ApplicationIdBuilder} instance supplier.
     * @return          this {@link StreamsExecutionEnvironment} instance.
     */
    T setApplicationIdBuilder(final Supplier<ApplicationIdBuilder> supplier);

    /**
     * Gets the {@link ApplicationIdBuilder}.
     *
     * @return          this {@link ApplicationIdBuilder} instance or {@code null}.
     */
    Supplier<ApplicationIdBuilder> getApplicationIdBuilder();

    /**
     * Adds streamsConfig that will be used in fallback if not present in defined environment streamsConfig.
     *
     * @param settings  the {@link Conf} instance.
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    T addFallbackConfiguration(final Conf settings);

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
     *
     * @throws IllegalStateException if the environment is not started.
     */
    void stop(boolean cleanUp);

    /**
     * Stops the streams instance for the specified application id.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @param cleanUp if local states of each {@link KafkaStreams} instance must be cleanup.
     *
     * @see KafkaStreams#cleanUp() .
     *
     * @throws IllegalStateException if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     */
    default void stop(final ApplicationId id, boolean cleanUp) {
        stop(id, cleanUp, Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Stops the streams instance for the specified application id.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @param cleanUp if local states of each {@link KafkaStreams} instance must be cleanup.
     * @param timeout the duration to wait for the streams to shutdown.
     *
     * @see KafkaStreams#cleanUp() .
     *
     * @throws IllegalStateException if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     */
    void stop(final ApplicationId id, boolean cleanUp, Duration timeout);

    /**
     * Stops the streams instance for the specified application id and remove the associated topology from this
     * environment.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @see KafkaStreams#cleanUp() .
     *
     * @throws IllegalStateException if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     */
    default void remove(final ApplicationId id) {
        remove(id, Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Stops the streams instance for the specified application id and remove the associated topology from this
     * environment.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @param timeout the duration to wait for the streams to shutdown.
     * @see KafkaStreams#cleanUp() .
     *
     * @throws IllegalStateException if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     */
    void remove(final ApplicationId id, final Duration timeout);
}

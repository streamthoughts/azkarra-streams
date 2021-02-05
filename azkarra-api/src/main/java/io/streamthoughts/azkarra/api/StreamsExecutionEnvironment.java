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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.NotFoundException;
import io.streamthoughts.azkarra.api.model.HasId;
import io.streamthoughts.azkarra.api.model.HasName;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsApplication;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import org.apache.kafka.streams.KafkaStreams;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A {@code StreamsExecutionEnvironment} manages the execution and the lifecycle of one or many {@link KafkaStreams}
 * instances that run either locally or remotely.
 *
 * @see KafkaStreamsContainer
 */
public interface StreamsExecutionEnvironment<T extends StreamsExecutionEnvironment<T>> extends HasName {

    /**
     * Gets the type of this {@link StreamsExecutionEnvironment}.
     *
     * @return the string type.
     */
    String type();

    /**
     * Gets the name of this {@link StreamsExecutionEnvironment}.
     *
     * @return the string name.
     */
    String name();

    /**
     * Gets the state of this {@link StreamsExecutionEnvironment}.
     *
     * @return the {@link State}.
     */
    State state();

    /**
     * Check whether this {@link StreamsExecutionEnvironment} is marked as default.
     *
     * @return {@code true} if this {@link StreamsExecutionEnvironment} is marked as default.
     */
    boolean isDefault();

    /**
     * Sets whether this execution environment should be the default.
     *
     * @param isDefault {@code true} to set this environment as default.
     *                              
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    T isDefault(final boolean isDefault);

    /**
     * Creates a new {@link StreamsTopologyExecution} to be applied on this {@link StreamsExecutionEnvironment}.
     *
     * @param meta     the {@link StreamsTopologyMeta} to executed.
     * @param executed the execution options.
     * @return the new {@link StreamsTopologyExecution} instance.
     */
    StreamsTopologyExecution newTopologyExecution(final StreamsTopologyMeta meta, final Executed executed);

    /**
     * Returns all containers for active Kafka Streams applications.
     *
     * @return a collection of {@link KafkaStreamsContainer} applications.
     */
    Collection<KafkaStreamsContainer> getContainers();

    /**
     * Gets a {@link KafkaStreamsContainer} for the specified id.
     *
     * @param id the {@link ContainerId}.
     * @return the {@link KafkaStreamsContainer}.
     */
    default KafkaStreamsContainer getContainerById(final ContainerId id) {
        return getContainers()
                .stream()
                .filter(c -> c.containerId().equals(id.id()))
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException(
                                "Failed to find running KafkaStreams instance for container id '"
                                        + id
                                        + "' in environment '" + name() + "'")
                );
    }

    /**
     * Returns all {@link ContainerId} for active Kafka Streams applications.
     *
     * @return the set of {@link ContainerId}.
     */
    Set<ContainerId> getContainerIds();

    /**
     * Returns all {{@link ApplicationId} for active Kafka Streams applications.
     *
     * @return the set of {@link ApplicationId}.
     */
    Set<ApplicationId> getApplicationIds();

    /**
     * Gets the {@link KafkaStreamsApplication} for the specified {@code application.id}
     *
     * @param id the {@code application.id}.
     * @return the {@link KafkaStreamsApplication} instance.
     */
    Optional<KafkaStreamsApplication> getApplicationById(final ApplicationId id);

    /**
     * Adds a new configuration to this environment.
     * This method can be invoked multiple time. The supplied configuration will override all prior configurations.
     *
     * @see #addConfiguration(Supplier)
     *
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    default T addConfiguration(final Conf configuration) {
        return addConfiguration(() -> configuration);
    }

    /**
     * Adds a new configuration to this environment.
     * This method can be invoked multiple time. The supplied configuration will override all prior configurations.
     *
     *
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    T addConfiguration(final Supplier<Conf> configuration);

    /**
     * Gets this environment configuration.
     *
     * @return the {@link Conf} instance.
     */
    Conf getConfiguration();

    /**
     * Sets the {@link ApplicationIdBuilder} that should be used for building streams {@code application.id}.
     *
     * @param supplier the {@link ApplicationIdBuilder} instance supplier.
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    T setApplicationIdBuilder(final Supplier<ApplicationIdBuilder> supplier);

    /**
     * Gets the {@link ApplicationIdBuilder}.
     *
     * @return this {@link ApplicationIdBuilder} instance or {@code null}.
     */
    Supplier<ApplicationIdBuilder> getApplicationIdBuilder();

    /**
     * Adds settings to this environment that will be used in fallback if not present
     * in the defined environment configuration.
     *
     * @param settings the {@link Conf} instance.
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
     * @throws IllegalStateException if the environment is not started.
     * @see KafkaStreams#cleanUp() .
     */
    void stop(boolean cleanUp);

    /**
     * Stops all the streams instances for the specified application id.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @param cleanUp if local states of each {@link KafkaStreams} instance must be cleanup.
     * @throws IllegalStateException    if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     * @see KafkaStreams#cleanUp() .
     */
    default void stop(final ApplicationId id, boolean cleanUp) {
        stop(id, cleanUp, Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Stops all the streams instance for the specified application id.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @param cleanUp if local states of each {@link KafkaStreams} instance must be cleanup.
     * @param timeout the duration to wait for the streams to shutdown.
     * @throws IllegalStateException    if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     * @see KafkaStreams#cleanUp() .
     */
    void stop(final ApplicationId id, boolean cleanUp, Duration timeout);

    /**
     * Stops the streams container for the specified {@code application.id}.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @param cleanUp if local states of each {@link KafkaStreams} instance must be cleanup.
     * @throws IllegalStateException    if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     * @see KafkaStreams#cleanUp() .
     */
    default void stop(final ContainerId id, boolean cleanUp) {
        stop(id, cleanUp, Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Stops the streams container for the specified {@code application.id}.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @param cleanUp if local states of each {@link KafkaStreams} instance must be cleanup.
     * @param timeout the duration to wait for the streams to shutdown.
     * @throws IllegalStateException    if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     * @see KafkaStreams#cleanUp() .
     */
    void stop(final ContainerId id, boolean cleanUp, Duration timeout);

    /**
     * Terminates all streams container for the specified {@code application.id} and remove the associated
     * topology from this environment.
     * <p>
     * A clean up of the local state directory is performed when the instance is terminated.
     *
     * @param id the {@link ContainerId} of the streams instance.
     * @throws IllegalStateException    if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     * @see KafkaStreams#cleanUp().
     */
    default void terminate(final ContainerId id) {
        terminate(id, Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Terminates the {@link KafkaStreams} instance for the specified {@code container.id} and remove the associated
     * topology from this environment.
     * <p>
     * A clean up of the local state directory is performed when the instance is terminated.
     *
     * @param id      the {@link ContainerId} of the streams instance.
     * @param timeout the duration to wait for the streams to shutdown.
     * @throws IllegalStateException    if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     * @see KafkaStreams#cleanUp().
     */
    void terminate(final ContainerId id, final Duration timeout);

    /**
     * Terminates all streams container for the specified {@code application.id} and remove the associated
     * topology from this environment.
     * <p>
     * A clean up of the local state directory of each container is performed when an application is terminated.
     *
     * @param id the {@link ApplicationId} of the streams instance.
     * @throws IllegalStateException    if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     * @see KafkaStreams#cleanUp().
     */
    default void terminate(final ApplicationId id) {
        terminate(id, Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Terminates all streams container for the specified {@code application.id} and remove the associated
     * topology from this environment.
     * <p>
     * A clean up of the local state directory of each container is performed when an application is terminated.
     *
     * @param id      the {@link ApplicationId} of the streams instance.
     * @param timeout the duration to wait for the streams to shutdown.
     * @throws IllegalStateException    if the environment is not started.
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     * @see KafkaStreams#cleanUp().
     */
    void terminate(final ApplicationId id, final Duration timeout);

    /**
     * Gets a serializable view of this {@link StreamsExecutionEnvironment} instance.
     * @return  the {@link View} backed by {@code this}.
     */
    default View describe() {
        return new View(this);
    }

    /**
     * A {@code Environment} is used to describe the current state of a
     * {@link io.streamthoughts.azkarra.api.StreamsExecutionEnvironment} instance.
     */
    class View implements HasName, Serializable {

        private final StreamsExecutionEnvironment<?> environment;

        /**
         * Creates a new {@link View} instance.
         *
         * @param environment the backing {@link StreamsExecutionEnvironment} object.
         */
        View(final StreamsExecutionEnvironment<?> environment) {
            this.environment = Objects.requireNonNull(environment, "environment should not be nul");
        }

        @JsonProperty("name")
        public String name() {
            return environment.name();
        }

        @JsonProperty("type")
        public String type() {
            return environment.type();
        }

        @JsonProperty("state")
        public State state() {
            return environment.state();
        }

        @JsonProperty("config")
        public Conf config() {
            return environment.getConfiguration();
        }

        @JsonProperty("applications")
        public Set<String> applications() {
            return HasId.getIds(environment.getApplicationIds());
        }

        @JsonProperty("is_default")
        public boolean isDefault() {
            return environment.isDefault();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof View)) return false;
            View that = (View) o;
            return isDefault() == that.isDefault() &&
                    Objects.equals(name(), that.name()) &&
                    Objects.equals(that, that.type()) &&
                    Objects.equals(config(), that.config()) &&
                    Objects.equals(applications(), that.applications());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(name(), config(), applications(), isDefault());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return environment.toString();
        }
    }
}

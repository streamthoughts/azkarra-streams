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
package io.streamthoughts.azkarra.api.streams;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.events.EventStream;
import io.streamthoughts.azkarra.api.events.reactive.EventStreamPublisher;
import io.streamthoughts.azkarra.api.model.Metric;
import io.streamthoughts.azkarra.api.model.MetricGroup;
import io.streamthoughts.azkarra.api.model.StreamsTopologyGraph;
import io.streamthoughts.azkarra.api.model.TimestampedValue;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.query.QueryableKafkaStreams;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerGroupOffsets;
import io.streamthoughts.azkarra.api.streams.store.LocalStatePartitionsInfo;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.api.util.Endpoint;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.processor.ThreadMetadata;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A {@code KafkaStreamsContainer} is used to encapsulate and to manipulate a {@link KafkaStreams} instance
 * that can be running either locally or remotely.
 */
public interface KafkaStreamsContainer extends QueryableKafkaStreams {

    /**
     * Restarts this container.
     */
    void restart();

    /**
     * Closes this {@link KafkaStreams} instance.
     */
    default void close(final Duration timeout) {
        close(false, timeout);
    }

    /**
     * Closes this {@link KafkaStreams} instance and wait up to the timeout for the streams to be closed.
     *
     * A {@code timeout} of 0 means to return immediately (i.e {@code Duration.ZERO}
     *
     * @param cleanUp flag to clean up the local streams states.
     * @param timeout the duration to wait for the streams to shutdown.
     *
     */
    void close(final boolean cleanUp, final Duration timeout);

    /**
     * Gets the current state of the streams container.
     *
     * @return  a {@link TimestampedValue} instance.
     */
    TimestampedValue<State> state();

    /**
     * Gets the local thread metadata.
     *
     * @return  a set of {@link ThreadMetadata} instance.
     */
    Set<ThreadMetadata> threadMetadata();

    /**
     * Gets the started epoch-time in milliseconds.
     *
     * @return  a unix epoch-time in milliseconds.
     */
    long startedSince();

    /**
     * Gets the configuration for this {@link KafkaStreams} instance.
     *
     * @return a {@link Conf} instance.
     */
    Conf streamsConfig();

    /**
     * Gets the id for this container.
     *
     * @return  a string container id.
     */
    String containerId();

    /**
     * Gets configured {@link StreamsConfig#APPLICATION_ID_CONFIG} for this {@link KafkaStreams} instance.
     *
     * @return  a string {@code application.id}
     */
    String applicationId();

    /**
     * Gets the endpoint configured for this {@link KafkaStreams} instance.
     *
     * @see StreamsConfig#APPLICATION_SERVER_CONFIG
     *
     * @return  an optional {@link Endpoint}
     */
    Optional<Endpoint> endpoint();

    /**
     * Gets the last observed exception thrown the {@link KafkaStreams} instance.
     *
     * @return a {@link Throwable} instance.
     */
    Optional<Throwable> exception();

    /**
     * Gets the {@link TopologyMetadata} about the topology runs by this {@link KafkaStreams} instance.
     *
     * @return  a {@link TopologyMetadata} instance.
     */
    TopologyMetadata topologyMetadata();

    /**
     * Gets the {@link StreamsTopologyGraph} for this {@link KafkaStreams} instance.
     *
     * @return  a new {@link TopologyDescription} instance.
     */
    StreamsTopologyGraph topologyGraph();

    /**
     * Gets all {@link Metric}s for this {@link KafkaStreams} instance.
     *
     * @see KafkaStreams#metrics()
     *
     * @return  a {@code Set} of {@link Metric}.
     */
    default Set<MetricGroup> metrics() {
        return metrics(KafkaMetricFilter.all());
    }

    /**
     * Gets all {@link Metric}s for this {@link KafkaStreams} instance matching the specified {@link KafkaMetricFilter}.
     *
     * @see KafkaStreams#metrics()
     *
     * @param   filter the {@link KafkaMetricFilter} to be used.
     * @return         a {@code Set} of {@link Metric}.
     */
    Set<MetricGroup> metrics(final KafkaMetricFilter filter);

    /**
     * Gets the offsets for the topic/partitions assigned to this {@link KafkaStreams} instance.
     * If the {@link KafkaStreams} instance is not running then no offsets will be computed.
     *
     * @return  the {@link ConsumerGroupOffsets}.
     */
    ConsumerGroupOffsets offsets();

    /**
     * Gets the default {@link Serde} configured for key.
     *
     * @return  a optional {@link Serde} instance.
     */
    Optional<Serde> defaultKeySerde();

    /**
     * Checks if the {@link KafkaStreams} is either RUNNING or REBALANCING.
     *
     * @return {@code false} if no {@link KafkaStreams} is initialized.
     */
    boolean isRunning() ;

    /**
     * Returns the wrapped {@link KafkaStreams} instance.
     *
     * This method can throw an {@link UnsupportedOperationException} if the {@link KafkaStreamsContainer}
     * implementation doesn't manage the {@link KafkaStreams} locally.
     *
     * @return  the {@link KafkaStreams}.
     */
    default KafkaStreams getKafkaStreams() {
        throw new UnsupportedOperationException();
    }
   /**
     * Returns the wrapped {@link Topology} instance.
     *
     * This method can throw an {@link UnsupportedOperationException} if the {@link KafkaStreamsContainer}
     * implementation doesn't manage the {@link KafkaStreams} locally.
     *
     * @return  the {@link Topology} instance.
     */
    default Topology getTopology() {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks whether the given {@link Endpoint} is the same as this container.
     *
     * @param endpoint  the {@link Endpoint} to verify.
     * @return          {@code true} if the given host
     */
    boolean checkEndpoint(final Endpoint endpoint);

    /**
     * Gets the partition restoration and lag for all local state store.
     *
     * @see KafkaStreams#allLocalStorePartitionLags().
     * @return the list of {@link LocalStatePartitionsInfo}.
     */
    List<LocalStatePartitionsInfo> allLocalStorePartitionInfos();

    /**
     * Describes the local {@code KafkaStreams} instance.
     *
     * @return  the {@link KafkaStreamsInstance} instance.
     */
    KafkaStreamsInstance describe();

    /**
     * Creates a new {@link Producer} instance using the same configs that the Kafka Streams instance.
     *
     * @param overrides the producer configs to overrides.
     */
    Producer<byte[], byte[]> createNewProducer(final Map<String, Object> overrides);

    /**
     * Gets a shared {@link AdminClient} instance for this {@link KafkaStreams} instance.
     *
     * @return a {@link AdminClient} instance.
     */
    AdminClient getAdminClient();

    /**
     * Gets a new {@link EventStreamPublisher} for the given event-tye.
     *
     * @param eventType the {@link EventStream} type.
     * @return          a new {@link EventStreamPublisher} instance.
     */
    <K, V> EventStreamPublisher<K, V> getEventStreamPublisherForType(final String eventType);

    /**
     * Registers a new {@link EventStream} to this container.
     *
     * @param eventStream   the {@link EventStream} to register.
     * @param <K>   the record key-type.
     * @param <V>   the record value-type.
     */
    <K, V> void registerEventStream(final EventStream<K, V> eventStream);

    /**
     * Gets the set of registered event streams.
     *
     * @return  the {@link Set} of event-streams.
     */
    Set<String> listRegisteredEventStreamTypes();

    /**
     * Register a watcher to be notified of {@link KafkaStreams.State} change event.
     *
     * @param watcher   the {@link StateChangeWatcher} to be registered.
     */
    void addStateChangeWatcher(final StateChangeWatcher watcher);

    /**
     * Watch a {@link KafkaStreams} instance for {@link KafkaStreams.State} change.
     *
     * By default, a {@link StateChangeWatcher} is one time called, i.e. once it is triggered,
     * it has to re-register itself to watch for further changes.
     */
    interface StateChangeWatcher {

        /**
         * Should this watcher be called for the given {@link State}.
         *
         * @param newState  the new state of the {@link KafkaStreams} instance.
         * @return          {@code true} if this watcher must be called, {@code false} otherwise.
         */
        default boolean accept(final State newState) {
            return true;
        }

        /**
         * Called when state changes. This method should not be blocking.
         *
         * @param event the {@link StateChangeEvent}
         */
        void onChange(final StateChangeEvent event);
    }

    /**
     * A {@code KafkaMetricFilter} can be used to only get specific metrics.
     */
    interface KafkaMetricFilter extends Predicate<Tuple<String, Metric>> {

        static KafkaMetricFilter of(final Predicate<Tuple<String, Metric>> predicate) {
            return predicate::test;
        }

        static KafkaMetricFilter all() {
            return candidate -> true;
        }

        static KafkaMetricFilter filterByGroup(final String groupName) {
            return candidate -> candidate.left().equals(groupName);
        }

        static KafkaMetricFilter filterByGroupAndMetricName(final String groupName, final String metricName) {
            return candidate -> candidate.left().equals(groupName) && candidate.right().name().equals(metricName);
        }
    }
}

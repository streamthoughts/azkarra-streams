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
import io.streamthoughts.azkarra.api.model.TimestampedValue;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerGroupOffsets;
import io.streamthoughts.azkarra.api.streams.store.LocalStorePartitionLags;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.processor.ThreadMetadata;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlySessionStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public interface KafkaStreamsContainer {

    /**
     * Asynchronously start the underlying {@link KafkaStreams} instance.
     *
     * @param executor the {@link Executor} instance to be used for starting the streams.
     */
    Future<State> start(final Executor executor);

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
     * Gets configured {@link StreamsConfig#APPLICATION_ID_CONFIG} for this {@link KafkaStreams} instance.
     *
     * @return  a string application.id.
     */
    String applicationId();

    /**
     * Gets configured {@link StreamsConfig#APPLICATION_SERVER_CONFIG} for this {@link KafkaStreams} instance.
     *
     * @return  a string application.server.
     */
    String applicationServer();

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
     * Gets the {@link TopologyDescription} for this {@link KafkaStreams} instance.
     *
     * @return  a new {@link TopologyDescription} instance.
     */
    TopologyDescription topologyDescription();

    /**
     * Gets all the current {@link Metric}s for this {@link KafkaStreams} instance.
     *
     * @see KafkaStreams#metrics()
     *
     * @return  a map of {@link Metric}.
     */
    Map<MetricName, ? extends Metric> metrics();

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
     * @return  the {@link KafkaStreams}.
     */
    KafkaStreams kafkaStreams();

    /**
     * Checks whether the given {@link HostInfo} is the same as this container.
     *
     * @param info  the {@link HostInfo} to verify.
     * @return      {@code true} if the given host
     */
    boolean isSameHost(final HostInfo info);

    /**
     * Gets the partition lag for all local state store.
     *
     * @see KafkaStreams#allLocalStorePartitionLags().
     * @return the list of {@link LocalStorePartitionLags}.
     */
    List<LocalStorePartitionLags> allLocalStorePartitionLags();

    Optional<ServerMetadata> localServerMetadata();

    /**
     * @see KafkaStreams#allMetadata().
     */
    Set<ServerMetadata> allMetadata();

    /**
     * @see KafkaStreams#allMetadataForStore(String).
     */
    Collection<ServerMetadata> allMetadataForStore(final String storeName);

    <K> Optional<KeyQueryMetadata> findMetadataForStoreAndKey(final String storeName,
                                                              final K key,
                                                              final Serializer<K> keySerializer);
    /**
     * Gets a read-only access to a local key-value store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlyKeyValueStore<K, V>> localKeyValueStore(final String store);

    /**
     * Gets a read-only access to the local timestamped key-value store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlyKeyValueStore<K, ValueAndTimestamp<V>>> localTimestampedKeyValueStore(final String store);

    /**
     * Gets a read-only access to a local window store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlyWindowStore<K, V>> localWindowStore(final String store);

    /**
     * Gets a read-only access to a local window store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlyWindowStore<K, ValueAndTimestamp<V>>> localTimestampedWindowStore(final String store);

    /**
     * Gets a read-only access to a local session store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlySessionStore<K, V>> localSessionStore(final String store);

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

    EventStreamPublisher eventStreamPublisherForType(final String eventType);

    <K, V> void registerEventStream(final EventStream<K, V> eventStream);

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
}

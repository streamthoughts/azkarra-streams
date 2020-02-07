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

import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.model.TimestampedValue;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.streams.topology.TopologyContainer;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.api.time.Time;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.errors.StreamsException;
import org.apache.kafka.streams.processor.ThreadMetadata;
import org.apache.kafka.streams.state.QueryableStoreType;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlySessionStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class KafkaStreamsContainer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamsContainer.class);

    private final KafkaStreamsFactory streamsFactory;

    private KafkaStreams kafkaStreams;

    private long started = -1;

    private volatile Throwable lastObservedException;

    private volatile TimestampedValue<State> state;

    private final TopologyContainer topologyContainer;

    private volatile Set<ThreadMetadata> threadMetadata;

    private final String applicationServer;

    private final LinkedBlockingQueue<StateChangeWatcher> stateChangeWatchers = new LinkedBlockingQueue<>();

    /**
     * The {@link Executor} which is used top start/stop the internal streams in a non-blocking way.
     */
    private Executor executor;

    /**
     * Creates a new {@link KafkaStreamsContainer} instance.
     *
     * @param topologyContainer the {@link TopologyContainer} instance.
     * @param streamsFactory    the {@link KafkaStreamsFactory} instance.
     */
    KafkaStreamsContainer(final TopologyContainer topologyContainer,
                          final KafkaStreamsFactory streamsFactory) {
        Objects.requireNonNull(topologyContainer, "topologyContainer cannot be null");
        Objects.requireNonNull(streamsFactory, "streamsFactory cannot be null");
        setState(State.NOT_CREATED);
        this.streamsFactory = streamsFactory;
        this.topologyContainer = topologyContainer;

        this.applicationServer = streamsConfig()
            .getOptionalString(StreamsConfig.APPLICATION_SERVER_CONFIG)
            .orElse(null);
    }

    /**
     * Asynchronously start the underlying {@link KafkaStreams} instance.
     *
     * @param executor the {@link Executor} instance to be used for starting the streams.
     *
     * @return  the future {@link org.apache.kafka.streams.KafkaStreams.State} of the streams.
     */
    public synchronized Future<KafkaStreams.State> start(final Executor executor) {
        this.executor = executor;
        started = Time.SYSTEM.milliseconds();
        kafkaStreams = streamsFactory.make(
            topologyContainer.topology(),
            topologyContainer.streamsConfig()
        );
        reset();
        setState(State.CREATED);
        // start() may block during a undefined period of time if the topology has defined GlobalKTables.
        // https://issues.apache.org/jira/browse/KAFKA-7380
        return CompletableFuture.supplyAsync(() -> {
            LOG.info("Initializing KafkaStreams container (application.id={})", applicationId());
            StreamsLifecycleChain streamsLifeCycle = new InternalStreamsLifeCycleChain(
                topologyContainer.interceptors().iterator(),
                (interceptor, chain) -> interceptor.onStart(new InternalStreamsLifeCycleContext(), chain),
                () -> {
                    try {
                        LOG.info("Starting KafkaStreams (application.id={})", applicationId());
                        kafkaStreams.start();
                    } catch (StreamsException e) {
                        lastObservedException = e;
                        throw e;
                    }
                }
            );
            streamsLifeCycle.execute();
            KafkaStreams.State state = kafkaStreams.state();
            LOG.info(
                "Completed KafkaStreams initialization (application.id={}, state={})",
                applicationId(),
                state
            );
            return state;

         }, executor);
    }

    private void reset() {
        lastObservedException = null;
    }

    private void setState(final State state) {
        this.state = new TimestampedValue<>(state);
    }

    /**
     * Gets the current state of the streams.
     *
     * @return  a {@link TimestampedValue} instance;
     */
    public TimestampedValue<State> state() {
        return state;
    }

    /**
     * Gets the default {@link Serde} configured for key.
     *
     * @return  a optional {@link Serde} instance.
     */
    public Optional<Serde> getDefaultKeySerde() {
        if (!streamsConfig().hasPath(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG)) {
            return Optional.empty();
        }
        return Try.failable(() ->
            streamsConfig().getClass(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serde.class)
        ).toOptional();
    }

    /**
     * Gets the local thread metadata.
     *
     * @return  a set of {@link ThreadMetadata} instance.
     */
    public Set<ThreadMetadata> threadMetadata() {
        return threadMetadata;
    }

    /**
     * Gets the started epoch-time in milliseconds.
     *
     * @return  a unix epoch-time in milliseconds.
     */
    public long startedSince() {
        return started;
    }

    /**
     * Gets the configuration for this {@link KafkaStreams} instance.
     *
     * @return a {@link Conf} instance.
     */
    public Conf streamsConfig() {
        return topologyContainer.streamsConfig();
    }

    /**
     * Gets configured {@link StreamsConfig#APPLICATION_ID_CONFIG} for this {@link KafkaStreams} instance.
     *
     * @return  a string application.id.
     */
    public String applicationId() {
        return streamsConfig().getString(StreamsConfig.APPLICATION_ID_CONFIG);
    }

    /**
     * Gets configured {@link StreamsConfig#APPLICATION_SERVER_CONFIG} for this {@link KafkaStreams} instance.
     *
     * @return  a string application.server.
     */
    public String applicationServer() {
        return applicationServer;
    }

    /**
     * Gets the last observed exception thrown the {@link KafkaStreams} instance.
     *
     * @return a {@link Throwable} instance.
     */
    public Optional<Throwable> exception() {
        return Optional.ofNullable(this.lastObservedException);
    }

    /**
     * Gets the {@link TopologyMetadata} about the topology runs by this {@link KafkaStreams} instance.
     *
     * @return  a {@link TopologyMetadata} instance.
     */
    public TopologyMetadata topologyMetadata() {
        return topologyContainer.metadata();
    }

    /**
     * Gets the {@link TopologyDescription} for this {@link KafkaStreams} instance.
     *
     * @return  a new {@link TopologyDescription} instance.
     */
    public TopologyDescription topologyDescription() {
        return topologyContainer.description();
    }

    /**
     * Gets all the current {@link Metric}s for this {@link KafkaStreams} instance.
     *
     * @return  a map of {@link Metric}.
     */
    public Map<MetricName, ? extends Metric> metrics() {
       return kafkaStreams.metrics();
    }

    /**
     * Closes this {@link KafkaStreams} instance.
     */
    public void close(final Duration timeout) {
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
    public void close(final boolean cleanUp, final Duration timeout) {
        if (cleanUp) reset();
        // close() method can be invoked from a StreamThread (i.e through UncaughtExceptionHandler),
        // to avoid thread deadlock streams instance should be closed using another thread.
        final Thread shutdownThread = new Thread(() -> {
            LOG.info("Closing KafkaStreams container (application.id={})", applicationId());
            StreamsLifecycleChain streamsLifeCycle = new InternalStreamsLifeCycleChain(
                topologyContainer.interceptors().iterator(),
                (interceptor, chain) -> interceptor.onStop(new InternalStreamsLifeCycleContext(), chain),
                () -> {
                    kafkaStreams.close();
                    if (cleanUp) {
                        LOG.info("Cleanup local states (application.id={})", applicationId());
                        kafkaStreams.cleanUp();
                    }
                    LOG.info("KafkaStreams closed completely (application.id={})", applicationId());
                }
            );
            streamsLifeCycle.execute();
        }, "kafka-streams-container-close-thread");
        shutdownThread.setDaemon(true);
        shutdownThread.start();

        final long waitMs = timeout.toMillis();
        if (waitMs > 0) {
            try {
                // This will cause a deadlock if the call is a StreamThread.
                shutdownThread.join(waitMs);
            } catch (InterruptedException e) {
                LOG.debug("Cannot transit to {} within {}ms", KafkaStreams.State.NOT_RUNNING, waitMs);
            }
        }
    }

    public void restart() {
        if (isNotRunning()) {
            restartNow(); // Restart internal streams immediately
        } else {
            // Register a watcher that will restart the streams as soon as the state is NOT_RUNNING.
            stateChangeWatchers.add(new StateChangeWatcher() {
                @Override
                public boolean accept(final KafkaStreams.State state) {
                    return state == KafkaStreams.State.NOT_RUNNING;
                }

                @Override
                public void apply() {
                    restartNow();
                }
            });
            // Do NOT clean-up states while restarting the streams.
            close(false, Duration.ZERO);
        }
    }

    private void restartNow() {
        CompletableFuture<KafkaStreams.State> f = (CompletableFuture<KafkaStreams.State>) start(executor);
        f.handle((state, throwable) -> {
            if (throwable != null) {
                LOG.error("Unexpected error happens while restarting streams", throwable);
            }
            return state;
        });
    }

    public Optional<StreamsServerInfo> getLocalServerInfo() {
        return getAllMetadata()
           .stream()
           .filter(StreamsServerInfo::isLocal)
           .findFirst();
    }

    public Set<StreamsServerInfo> getAllMetadata() {
        if (isNotRunning()) {
            return Collections.emptySet();
        }
        // allMetadata throw an IllegalAccessException if instance is not running
        return kafkaStreams.allMetadata()
           .stream()
           .map(this::newServerInfoFor)
           .collect(Collectors.toSet());
    }

    public Collection<StreamsServerInfo> getAllMetadataForStore(final String storeName) {
        Objects.requireNonNull(storeName, "storeName cannot be null");
        Collection<StreamsMetadata> metadata = kafkaStreams.allMetadataForStore(storeName);
        return metadata.stream()
            .map(this::newServerInfoFor)
            .collect(Collectors.toList());
    }

    public <K> Optional<StreamsServerInfo> getMetadataForStoreAndKey(final String storeName,
                                                           final K key,
                                                           final Serializer<K> keySerializer) {
        Objects.requireNonNull(storeName, "storeName cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(keySerializer, "keySerializer cannot be null");
        StreamsMetadata metadata = kafkaStreams.metadataForKey(storeName, key, keySerializer);
        return metadata == null || metadata.equals(StreamsMetadata.NOT_AVAILABLE) ?
            Optional.empty(): Optional.of(newServerInfoFor(metadata));
    }

    public <K, V> LocalStoreAccessor<ReadOnlyKeyValueStore<K, V>> getLocalKeyValueStore(final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.keyValueStore());
    }

    public <K, V> LocalStoreAccessor<ReadOnlyKeyValueStore<K, ValueAndTimestamp<V>>> getLocalTimestampedKeyValueStore(
            final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.timestampedKeyValueStore());
    }

    public <K, V> LocalStoreAccessor<ReadOnlyWindowStore<K, V>> getLocalWindowStore(final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.windowStore());
    }

    public <K, V> LocalStoreAccessor<ReadOnlySessionStore<K, V>> getLocalSessionStore(final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.sessionStore());
    }

    public <T> LocalStoreAccessor<T> getLocalStoreAccess(final String storeName,
                                                         final QueryableStoreType<T> storeType) {
        return new LocalStoreAccessor<>(() -> kafkaStreams.store(storeName, storeType));
    }

    Logger logger() {
        return LOG;
    }

    public boolean isNotRunning() {
        return !kafkaStreams.state().isRunning();
    }

    void stateChanges(final long now,
                      final KafkaStreams.State newState,
                      final KafkaStreams.State oldstate) {
        state = new TimestampedValue<>(now, State.valueOf(newState.name()));
        if (newState == KafkaStreams.State.RUNNING) {
            threadMetadata = kafkaStreams.localThreadsMetadata();
        } else {
            threadMetadata = Collections.emptySet();
        }

        if (!stateChangeWatchers.isEmpty()) {
            List<StateChangeWatcher> watchers = new ArrayList<>(stateChangeWatchers.size());
            stateChangeWatchers.drainTo(watchers);
            for (StateChangeWatcher listener : watchers) {
                if (listener.accept(newState)) {
                    listener.apply();
                } else {
                    stateChangeWatchers.add(listener);
                }
            }
        }
    }

    void setException(final Throwable throwable) {
        lastObservedException = throwable;
    }

    private StreamsServerInfo newServerInfoFor(final StreamsMetadata metadata) {
        return new StreamsServerInfo(
            applicationId(),
            metadata.host(),
            metadata.port(),
            metadata.stateStoreNames(),
            groupByTopicThenGet(metadata.topicPartitions()),
            isLocal(metadata)
        );
    }

    private boolean isLocal(final StreamsMetadata metadata) {
        return (metadata.host() + ":" + metadata.port()).equals(applicationServer);
    }

    private static Set<TopicPartitions> groupByTopicThenGet(final Set<TopicPartition> topicPartitions) {

        return topicPartitions
            .stream()
            .collect(Collectors.groupingBy(TopicPartition::topic))
            .entrySet()
            .stream()
            .map( entry -> new TopicPartitions(
               entry.getKey(),
               entry.getValue().stream().map(TopicPartition::partition).collect(Collectors.toSet()))
            ).collect(Collectors.toSet());
    }

    private interface StateChangeWatcher {

        boolean accept(final KafkaStreams.State state);

        void apply();
    }

    public class InternalStreamsLifeCycleContext implements StreamsLifecycleContext {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getApplicationId() {
            return applicationId();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TopologyDescription getTopology() {
            return topologyContainer.description();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Conf getStreamConfig() {
            return topologyContainer.streamsConfig();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public State getState() {
            return state().value();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setState(final State state) {
            KafkaStreamsContainer.this.setState(state);
        }
    }
}
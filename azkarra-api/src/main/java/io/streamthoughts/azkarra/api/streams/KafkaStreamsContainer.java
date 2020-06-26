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
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.model.TimestampedValue;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerClientOffsets;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerGroupOffsets;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerLogOffsets;
import io.streamthoughts.azkarra.api.streams.consumer.GlobalConsumerOffsetsRegistry;
import io.streamthoughts.azkarra.api.streams.consumer.LogOffsetsFetcher;
import io.streamthoughts.azkarra.api.streams.internal.InternalStreamsLifecycleContext;
import io.streamthoughts.azkarra.api.streams.topology.TopologyContainer;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.api.time.Time;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG;

public class KafkaStreamsContainer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamsContainer.class);

    private final KafkaStreamsFactory streamsFactory;

    private KafkaStreams kafkaStreams;

    private long started = -1;

    private volatile Throwable lastObservedException;

    private volatile TimestampedValue<State> state;

    private final TopologyContainer topologyContainer;

    private volatile Set<ThreadMetadata> threadMetadata = Collections.emptySet();

    private final String applicationServer;

    private final LinkedBlockingQueue<StateChangeWatcher> stateChangeWatchers = new LinkedBlockingQueue<>();

    private KafkaConsumer<byte[], byte[]> consumer;

    private volatile ContainerState containerState;
    private final Object containerStateLock = new Object();

    /**
     * The {@link Executor} which is used top start/stop the internal streams in a non-blocking way.
     */
    private Executor executor;

    // The internal states used to manage container Lifecycle
    private enum ContainerState {
        CREATED(1, 3),                  // 0
        STARTING(2),                    // 1
        STARTED(3),                     // 2
        PENDING_SHUTDOWN(4),            // 3
        STOPPED(1, 3);                  // 4

        private final Set<Integer> validTransitions = new HashSet<>();
        ContainerState(final Integer... validTransitions) {
            this.validTransitions.addAll(Arrays.asList(validTransitions));
        }

        public boolean isValidTransition(final ContainerState newState) {
            return validTransitions.contains(newState.ordinal());
        }
    }

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
        containerState = ContainerState.CREATED;
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
        LOG.info("Starting KafkaStreams container for name='{}', version='{}', id='{}'.",
            topologyContainer.metadata().name(),
            topologyContainer.metadata().version(),
            applicationId());

        setContainerState(ContainerState.STARTING);
        started = Time.SYSTEM.milliseconds();
        reset();
        this.executor = executor;
        stateChangeWatchers.clear(); // Remove all watchers that was registered during a previous run.
        kafkaStreams = streamsFactory.make(
            topologyContainer.topology(),
            topologyContainer.streamsConfig()
        );
        setState(State.CREATED);
        // start() may block during a undefined period of time if the topology has defined GlobalKTables.
        // https://issues.apache.org/jira/browse/KAFKA-7380
        return CompletableFuture.supplyAsync(() -> {
            LOG.info("Executing stream-lifecycle interceptor chain (id={})", applicationId());
            StreamsLifecycleChain streamsLifeCycle = new InternalStreamsLifeCycleChain(
                topologyContainer.interceptors().iterator(),
                (interceptor, chain) -> interceptor.onStart(new InternalStreamsLifecycleContext(this), chain),
                () -> {
                    try {
                        LOG.info("Starting KafkaStreams (id={})", applicationId());
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
                "Completed KafkaStreamsContainer initialization (id={}, state={})",
                applicationId(),
                state
            );
            setContainerState(ContainerState.STARTED);
            return state;

         }, executor);
    }

    private void reset() {
        lastObservedException = null;
    }

    /**
     * Sets the current state of the streams.
     *
     * @param state the KafkaStreams state.
     */
    public void setState(final State state) {
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
       return initialized() ? kafkaStreams.metrics() : Collections.emptyMap();
    }

    /**
     * Gets the offsets for the topic/partitions assigned to this {@link KafkaStreams} instance.
     * If the {@link KafkaStreams} instance is not running then no offsets will be computed.
     *
     * @return  the {@link ConsumerGroupOffsets}.
     */
    public ConsumerGroupOffsets offsets() {
        if (isNotRunning()) {
            return new ConsumerGroupOffsets(applicationId(), Collections.emptySet());
        }

        final ConsumerGroupOffsets consumerGroupOffsets = GlobalConsumerOffsetsRegistry
            .getInstance()
            .offsetsFor(applicationId())
            .snapshot();

        final Set<TopicPartition> activeTopicPartitions = threadMetadata()
            .stream()
            .flatMap(t -> t.activeTasks().stream())
            .flatMap(t -> t.topicPartitions().stream())
            .collect(Collectors.toSet());

        final Map<TopicPartition, Long> logEndOffsets = LogOffsetsFetcher.fetchLogEndOffsetsFor(
            getConsumer(),
            activeTopicPartitions
        );

        final Map<TopicPartition, Long> logStartOffsets = LogOffsetsFetcher.fetchLogStartOffsetsFor(
            getConsumer(),
            activeTopicPartitions
        );

        final Set<ConsumerClientOffsets> consumerAndOffsets = consumerGroupOffsets.consumers()
            .stream()
            .map(client -> {
                Set<ConsumerLogOffsets> offsets = client.positions()
                    .stream()
                    .map(logOffsets -> {
                        if (!activeTopicPartitions.contains(logOffsets.topicPartition()))
                            return null;
                        return logOffsets
                            .logEndOffset(logEndOffsets.get(logOffsets.topicPartition()))
                            .logStartOffset(logStartOffsets.get(logOffsets.topicPartition()));
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                return new ConsumerClientOffsets(client.clientId(), client.streamThread(), offsets);
            })
            .collect(Collectors.toSet());
        return new ConsumerGroupOffsets(consumerGroupOffsets.group(), consumerAndOffsets);
    }

    /**
     * Creates a new {@link Producer} instance using the same configs that the Kafka Streams instance.
     *
     * @param overrides the producer configs to overrides.
     */
    public Producer<byte[], byte[]> getProducer(final Map<String, Object> overrides) {
        String producerClientId = (String) overrides.get(ProducerConfig.CLIENT_ID_CONFIG);
        if (producerClientId == null) {
            final UUID containerId = UUID.randomUUID();
            final String clientId = streamsConfig()
                    .getOptionalString(StreamsConfig.CLIENT_ID_CONFIG)
                    .orElse(applicationId());
            producerClientId = clientId + "-" + containerId + "-producer";
        }
        Map<String, Object> props = getProducerConfigs(streamsConfig().getConfAsMap());
        props.putAll(overrides);
        props.put(BOOTSTRAP_SERVERS_CONFIG, streamsConfig().getString(BOOTSTRAP_SERVERS_CONFIG));
        props.put(CLIENT_ID_CONFIG, producerClientId);
        return new KafkaProducer<>(props, new ByteArraySerializer(), new ByteArraySerializer());
    }

    /**
     * Gets an {@link Consumer} instance for this {@link KafkaStreams} instance.
     *
     * @return a {@link Consumer} instance.
     */
    private synchronized Consumer<byte[], byte[]> getConsumer() {
        if (consumer == null) {
            final UUID containerId = UUID.randomUUID();
            final String clientId = streamsConfig()
                    .getOptionalString(StreamsConfig.CLIENT_ID_CONFIG)
                    .orElse(applicationId());
            final String consumerClientId = clientId + "-" + containerId + "-consumer";
            Map<String, Object> props = getConsumerConfigs(streamsConfig().getConfAsMap());
            props.put(BOOTSTRAP_SERVERS_CONFIG, streamsConfig().getString(BOOTSTRAP_SERVERS_CONFIG));
            props.put(CLIENT_ID_CONFIG, consumerClientId);
            // no need to set group id for a internal consumer
            props.remove(ConsumerConfig.GROUP_ID_CONFIG);
            consumer = new KafkaConsumer<>(props, new ByteArrayDeserializer(), new ByteArrayDeserializer());
        }
        return consumer;
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
        closeAndOptionallyRestart(cleanUp, timeout, false);
    }

    private void closeAndOptionallyRestart(final boolean cleanUp,
                                           final Duration timeout,
                                           final boolean restartAfterClose) {
        validateInitialized();

        boolean proceed = true;
        synchronized (containerStateLock) {
            if (!setContainerState(ContainerState.PENDING_SHUTDOWN)) {
                LOG.warn(
                    "KafkaStreamsContainer is already in the pending shutdown state, wait to complete shutdown (id={})",
                    applicationId());
                proceed = false;
            }
        }

        if (proceed) {

            if (restartAfterClose) {
                // Register a watcher that will restart this container as soon as its state is STOPPED.
                stateChangeWatchers.add(new StateChangeWatcher() {
                    @Override
                    public boolean accept(final State state) {
                        return state == State.STOPPED;
                    }

                    @Override
                    public void onChange(final StateChangeEvent event) {
                        restartNow();
                    }
                });
            }

            if (cleanUp) reset();
            // close() method can be invoked from a StreamThread (i.e through UncaughtExceptionHandler),
            // to avoid thread deadlock streams instance should be closed using another thread.
            final Thread shutdownThread = new Thread(() -> {
                LOG.info("Closing KafkaStreamsContainer (id={})", applicationId());
                StreamsLifecycleChain streamsLifeCycle = new InternalStreamsLifeCycleChain(
                    topologyContainer.interceptors().iterator(),
                    (interceptor, chain) -> interceptor.onStop(new InternalStreamsLifecycleContext(this), chain),
                    () -> {
                        kafkaStreams.close();
                        if (cleanUp) {
                            LOG.info("Cleanup local states (id={})", applicationId());
                            kafkaStreams.cleanUp();
                        }
                        LOG.info("KafkaStreams closed completely (id={})", applicationId());
                    }
                );
                streamsLifeCycle.execute();
                closeInternals();
                LOG.info("KafkaStreamsContainer has been closed (id={})", applicationId());
                // This may trigger a container restart
                setContainerState(ContainerState.STOPPED);
                stateChanges(new StateChangeEvent(State.STOPPED, State.valueOf(kafkaStreams.state().name())));
            }, "kafka-streams-container-close-thread");

            shutdownThread.setDaemon(true);
            shutdownThread.start();
        }

        final long waitMs = timeout.toMillis();
        if (waitMs > 0 && !waitOnContainerState(ContainerState.STOPPED, waitMs)) {
            LOG.debug(
                "KafkaStreamsContainer cannot transit to {} within {}ms (id={})",
                ContainerState.STOPPED,
                waitMs,
                applicationId()
            );
        }
    }

    private boolean setContainerState(final ContainerState newState) {
        synchronized (containerStateLock) {
            if (!containerState.isValidTransition(newState)) {
                if (containerState == ContainerState.PENDING_SHUTDOWN && newState == ContainerState.PENDING_SHUTDOWN) {
                    return false;
                }
                throw new IllegalStateException("KafkaStreamsContainer " + applicationId() + ": " +
                        "Unexpected state transition from " + containerState + " to " + newState);
            }
            containerState = newState;
            containerStateLock.notifyAll();
            return true;
        }
    }

    private boolean waitOnContainerState(final ContainerState targetState,
                                         final long waitMs) {
        final long start = Time.SYSTEM.milliseconds();
        synchronized (containerStateLock) {
            long elapsedMs = 0L;
            while (containerState != targetState) {
                if (waitMs > elapsedMs) {
                    final long remainingMs = waitMs - elapsedMs;
                    try {
                        containerStateLock.wait(remainingMs);
                    } catch (final InterruptedException ignore) {
                    }
                } else {
                    return false;
                }
                elapsedMs = Time.SYSTEM.milliseconds() - start;
            }
            return true;
        }
    }

    public void restart() {
        // It seems to be safe to restart this container immediately
        if (containerState.isValidTransition(ContainerState.STARTING)) {
            restartNow();
        // Else we should ensure that all the container resources are properly closed before restarting.
        } else {
            // Do NOT clean-up states while restarting the streams.
            closeAndOptionallyRestart(false, Duration.ZERO, true);
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
        if (isNotRunning()) return Collections.emptySet();

        // allMetadata throw an IllegalAccessException if instance is not running
        return kafkaStreams.allMetadata()
           .stream()
           .map(this::newServerInfoFor)
           .collect(Collectors.toSet());
    }

    public Collection<StreamsServerInfo> getAllMetadataForStore(final String storeName) {
        Objects.requireNonNull(storeName, "storeName cannot be null");
        if (isNotRunning()) return Collections.emptySet();

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

        if (!initialized()) return Optional.empty();

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

    public <K, V> LocalStoreAccessor<ReadOnlyWindowStore<K, ValueAndTimestamp<V>>> getLocalTimestampedWindowStore(
            final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.timestampedWindowStore());
    }

    public <K, V> LocalStoreAccessor<ReadOnlySessionStore<K, V>> getLocalSessionStore(final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.sessionStore());
    }

    private <T> LocalStoreAccessor<T> getLocalStoreAccess(final String storeName,
                                                          final QueryableStoreType<T> storeType) {
        return new LocalStoreAccessor<>(() -> kafkaStreams.store(storeName, storeType));
    }

    Logger logger() {
        return LOG;
    }

    /**
     * Checks if the {@link KafkaStreams} is neither RUNNING nor REBALANCING.
     *
     * @return {@code true} if no {@link KafkaStreams} is initialized.
     */
    public boolean isNotRunning() {
        if (!initialized()) return true;

        // This is equivalent to the KafkaStreams methods :
        // State.isRunning() <= 2.4 or State.isRunningOrRebalancing() >= 2.5
        final KafkaStreams.State state = kafkaStreams.state();
        return !(state.equals(KafkaStreams.State.RUNNING) || state.equals(KafkaStreams.State.REBALANCING));
    }

    /**
     * Register a watcher to be notified of {@link KafkaStreams.State} change event.
     *
     * @param watcher   the {@link StateChangeWatcher} to be registered.
     */
    public void addStateChangeWatcher(final StateChangeWatcher watcher) {
        stateChangeWatchers.add(Objects.requireNonNull(watcher, "Cannot register null watcher"));
    }

    void stateChanges(final StateChangeEvent stateChangeEvent) {
        state = new TimestampedValue<>(stateChangeEvent.timestamp(), stateChangeEvent.newState());
        if (state.value() == State.RUNNING) {
            threadMetadata = kafkaStreams.localThreadsMetadata();
        } else {
            threadMetadata = Collections.emptySet();
        }

        if (!stateChangeWatchers.isEmpty()) {
            List<StateChangeWatcher> watchers = new ArrayList<>(stateChangeWatchers.size());
            stateChangeWatchers.drainTo(watchers);
            for (StateChangeWatcher watcher : watchers) {
                if (watcher.accept(stateChangeEvent.newState())) {
                    watcher.onChange(stateChangeEvent);
                } else {
                    stateChangeWatchers.add(watcher);
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

    private static Map<String, Object> getConsumerConfigs(final Map<String, Object> configs) {
        final Map<String, Object> parsed = new HashMap<>();
        for (final String configName: ConsumerConfig.configNames()) {
            if (configs.containsKey(configName)) {
                parsed.put(configName, configs.get(configName));
            }
        }
        return parsed;
    }

    private static Map<String, Object> getProducerConfigs(final Map<String, Object> configs) {
        final Map<String, Object> parsed = new HashMap<>();
        for (final String configName: ProducerConfig.configNames()) {
            if (configs.containsKey(configName)) {
                parsed.put(configName, configs.get(configName));
            }
        }
        return parsed;
    }

    /**
     * Returns the wrapper {@link KafkaStreams} instance.
     *
     * @return  the {@link KafkaStreams}.
     */
    public KafkaStreams getKafkaStreams() {
        validateInitialized();
        return kafkaStreams;
    }

    private void validateInitialized() {
        if (!initialized())
            throw new IllegalStateException(
                "This container is not started. Cannot get access to KafkaStreams instance.");
    }

    private void closeInternals() {
        LOG.info("Closing internal clients for Kafka Streams container (application.id={})", applicationId());
        try {
            if (consumer != null) consumer.close();
        } catch (Exception e) {
            LOG.error("Unexpected error occurred while closing internal resources", e);
        } finally {
            consumer = null;
        }
    }

    /**
     * @return {@code true} if the {@link KafkaStreams} is not equal {@code null},
     *                      i.e container has been started at least once.
     */
    private boolean initialized() {
        return kafkaStreams != null;
    }

    /**
     * Watch a {@link KafkaStreams} instance for {@link KafkaStreams.State} change.
     *
     * By default, a {@link StateChangeWatcher} is one time called, i.e. once it is triggered,
     * it has to re-register itself to watch for further changes.
     */
    public interface StateChangeWatcher {

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
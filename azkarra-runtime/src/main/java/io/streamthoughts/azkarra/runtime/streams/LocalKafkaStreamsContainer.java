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
package io.streamthoughts.azkarra.runtime.streams;

import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.events.EventStream;
import io.streamthoughts.azkarra.api.events.reactive.AsyncMulticastEventStreamPublisher;
import io.streamthoughts.azkarra.api.events.reactive.EventStreamPublisher;
import io.streamthoughts.azkarra.api.model.MetricGroup;
import io.streamthoughts.azkarra.api.model.StreamsTopologyGraph;
import io.streamthoughts.azkarra.api.model.TimestampedValue;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsFactory;
import io.streamthoughts.azkarra.api.streams.ServerHostInfo;
import io.streamthoughts.azkarra.api.streams.ServerMetadata;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.streams.StateChangeEvent;
import io.streamthoughts.azkarra.api.streams.TopicPartitions;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerClientOffsets;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerGroupOffsets;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerLogOffsets;
import io.streamthoughts.azkarra.api.streams.consumer.GlobalConsumerOffsetsRegistry;
import io.streamthoughts.azkarra.api.streams.consumer.LogOffsetsFetcher;
import io.streamthoughts.azkarra.api.streams.internal.InternalStreamsLifeCycleChain;
import io.streamthoughts.azkarra.api.streams.store.LocalStorePartitionLags;
import io.streamthoughts.azkarra.api.streams.store.PartitionLogOffsetsAndLag;
import io.streamthoughts.azkarra.api.streams.topology.TopologyDefinition;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.api.time.Time;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
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
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsException;
import org.apache.kafka.streams.processor.ThreadMetadata;
import org.apache.kafka.streams.state.HostInfo;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

import static java.util.stream.Collectors.joining;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG;
import static org.apache.kafka.streams.StoreQueryParameters.fromNameAndType;

public class LocalKafkaStreamsContainer implements KafkaStreamsContainer {

    private static final Logger LOG = LoggerFactory.getLogger(LocalKafkaStreamsContainer.class);

    private final KafkaStreamsFactory streamsFactory;

    private KafkaStreams kafkaStreams;

    private final Conf streamsConfig;

    private long started = -1;

    private volatile Throwable lastObservedException;

    private volatile TimestampedValue<State> state;

    private final TopologyDefinition topologyDefinition;

    private volatile Set<ThreadMetadata> threadMetadata = Collections.emptySet();

    private final String applicationServer;

    private final LinkedBlockingQueue<StateChangeWatcher> stateChangeWatchers = new LinkedBlockingQueue<>();

    private final List<StreamsLifecycleInterceptor> interceptors;

    private KafkaConsumer<byte[], byte[]> consumer;

    private AdminClient adminClient;

    private volatile ContainerState containerState;
    private final Object containerStateLock = new Object();

    private final List<EventStream> eventStreams = new LinkedList<>();

    private final Map<String, EventStreamPublisher> publishers = new LinkedHashMap<>();

    /**
     * The {@link Executor} which is used top start/stop the internal streams in a non-blocking way.
     */
    private Executor executor;
    private final UUID containerId;

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
     * @return a new {@link LocalKafkaStreamsContainerBuilder} instance.
     */
    public static LocalKafkaStreamsContainerBuilder newBuilder() {
        return new LocalKafkaStreamsContainerBuilder();
    }

    /**
     * Creates a new {@link LocalKafkaStreamsContainer} instance.
     *
     * @param topologyDefinition the {@link TopologyDefinition} instance.
     * @param streamsFactory     the {@link KafkaStreamsFactory} instance.
     */
    LocalKafkaStreamsContainer(final Conf streamsConfig,
                               final TopologyDefinition topologyDefinition,
                               final KafkaStreamsFactory streamsFactory,
                               final List<StreamsLifecycleInterceptor> interceptors) {
        Objects.requireNonNull(topologyDefinition, "topologyDefinition cannot be null");
        Objects.requireNonNull(streamsFactory, "streamsFactory cannot be null");
        this.streamsConfig = Objects.requireNonNull(streamsConfig, "streamConfigs cannot be null");
        containerState = ContainerState.CREATED;
        setState(State.Standards.NOT_CREATED);
        this.interceptors = interceptors;
        this.streamsFactory = streamsFactory;
        this.topologyDefinition = topologyDefinition;
        this.containerId = UUID.randomUUID();
        this.applicationServer = streamsConfig()
            .getOptionalString(StreamsConfig.APPLICATION_SERVER_CONFIG)
            .orElse(null);
        topologyDefinition.getEventStreams().forEach(this::registerEventStream);
    }

    /**
     *Asynchronously start the underlying {@link KafkaStreams} instance.
     *
     * @param executor the {@link Executor} instance to be used for starting the streams.
     */
    public Future<State> start(final Executor executor) {
        LOG.info("Starting KafkaStreams container for name='{}', version='{}', id='{}'.",
            topologyDefinition.getName(),
            topologyDefinition.getVersion(),
            applicationId());

        LOG.info("StreamsLifecycleInterceptors : {}",
        interceptors
            .stream()
            .map(StreamsLifecycleInterceptor::name)
            .collect(joining("\n\t", "\n\t", "")));

        setContainerState(ContainerState.STARTING);
        started = Time.SYSTEM.milliseconds();
        reset();
        this.executor = executor;
        stateChangeWatchers.clear(); // Remove all watchers that was registered during a previous run.
        kafkaStreams = streamsFactory.make(
            topologyDefinition.getTopology(),
            streamsConfig
        );

        for (EventStream<? ,?> stream : eventStreams) {
            var eventType = stream.type();
            if (publishers.put(eventType, new AsyncMulticastEventStreamPublisher<>(stream)) != null) {
                throw new AzkarraException("Cannot register two event-streams for type: " + eventType);
            }
        }

        setState(State.Standards.CREATED);
        // start() may block during a undefined period of time if the topology has defined GlobalKTables.
        // https://issues.apache.org/jira/browse/KAFKA-7380
        return CompletableFuture.supplyAsync(() -> {
            LOG.info("Executing stream-lifecycle interceptor chain (id={})", applicationId());
            StreamsLifecycleChain streamsLifeCycle = new InternalStreamsLifeCycleChain(
                interceptors.iterator(),
                (interceptor, chain) -> interceptor.onStart(newStreamsLifecycleContext(), chain),
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
            return state().value();

         }, executor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventStreamPublisher eventStreamPublisherForType(final String eventType) {
        Objects.requireNonNull(eventType, "eventType cannot be null");
        validateInitialized();
        if (!publishers.containsKey(eventType)) {
            throw new IllegalArgumentException("Cannot found Event-Stream for type: " + eventType);
        }
        return publishers.get(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> void registerEventStream(final EventStream<K, V> eventStream) {
        eventStreams.add(Objects.requireNonNull(eventStream, "eventStream cannot be null"));
    }

    private void reset() {
        lastObservedException = null;
        publishers.clear();
    }

    /**
     * Sets the current state of the streams.
     *
     * @param state the KafkaStreams state.
     */
    private void setState(final State state) {
        this.state = new TimestampedValue<>(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimestampedValue<State> state() {
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Serde> defaultKeySerde() {
        if (!streamsConfig().hasPath(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG)) {
            return Optional.empty();
        }
        return Try.failable(() ->
            streamsConfig().getClass(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serde.class)
        ).toOptional();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ThreadMetadata> threadMetadata() {
        return threadMetadata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long startedSince() {
        return started;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf streamsConfig() {
        return streamsConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String applicationId() {
        return streamsConfig().getString(StreamsConfig.APPLICATION_ID_CONFIG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String applicationServer() {
        return applicationServer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Throwable> exception() {
        return Optional.ofNullable(this.lastObservedException);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopologyMetadata topologyMetadata() {
        return new TopologyMetadata(
            topologyDefinition.getName(),
            topologyDefinition.getVersion(),
            topologyDefinition.getDescription()
        );
    }

    /**
     * {@inheritDoc}
     */
    public StreamsTopologyGraph topologyGraph() {
        return StreamsTopologyGraph.build(topologyDefinition.getTopology().describe());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<MetricGroup> metrics(final KafkaMetricFilter filter) {
        final Map<MetricName, ? extends Metric> kafkaMetrics = initialized()
            ? kafkaStreams.metrics() :
            Collections.emptyMap();

        Map<String, List<io.streamthoughts.azkarra.api.model.Metric>> m = new HashMap<>(kafkaMetrics.size());
        for (Map.Entry<MetricName, ? extends org.apache.kafka.common.Metric> elem : kafkaMetrics.entrySet()) {
            final MetricName metricName = elem.getKey();
            final org.apache.kafka.common.Metric metricValue = elem.getValue();

            final io.streamthoughts.azkarra.api.model.Metric metric = new io.streamthoughts.azkarra.api.model.Metric(
                metricName.name(),
                metricName.group(),
                metricName.description(),
                metricName.tags(),
                metricValue.metricValue()
            );
            final boolean filtered = filter.test(Tuple.of(metricName.group(), metric));
            if (filtered) {
                m.computeIfAbsent(metricName.group(), k -> new LinkedList<>()).add(metric);
            }
        }

        return m.entrySet()
            .stream()
            .map(e -> new MetricGroup(e.getKey(), e.getValue()))
            .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerGroupOffsets offsets() {
        if (!isRunning()) {
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
     * {@inheritDoc}
     */
    @Override
    public Producer<byte[], byte[]> createNewProducer(final Map<String, Object> overrides) {
        String producerClientId = (String) overrides.get(ProducerConfig.CLIENT_ID_CONFIG);
        if (producerClientId == null) {
            final String clientId = streamsConfig()
                    .getOptionalString(StreamsConfig.CLIENT_ID_CONFIG)
                    .orElse(applicationId());
            producerClientId = clientId + "-" + containerId + "-producer";
        }
        Map<String, Object> props = getProducerConfigs(streamsConfig.getConfAsMap());
        props.putAll(overrides);
        props.put(BOOTSTRAP_SERVERS_CONFIG, streamsConfig.getString(BOOTSTRAP_SERVERS_CONFIG));
        props.put(CLIENT_ID_CONFIG, producerClientId);
        return new KafkaProducer<>(props, new ByteArraySerializer(), new ByteArraySerializer());
    }

    /**
     * Gets a shared {@link Consumer} instance for this {@link KafkaStreams} instance.
     *
     * @return a {@link Consumer} instance.
     */
    private synchronized Consumer<byte[], byte[]> getConsumer() {
        if (consumer == null) {
            var configs = getConsumerConfigs(streamsConfig.getConfAsMap());

            var clientId = streamsConfig().getOptionalString(CLIENT_ID_CONFIG).orElse(applicationId());
            var consumerClientId = clientId + "-" + containerId + "-consumer";
            configs.put(BOOTSTRAP_SERVERS_CONFIG, streamsConfig.getString(BOOTSTRAP_SERVERS_CONFIG));
            configs.put(CLIENT_ID_CONFIG, consumerClientId);
            // no need to set group id for a internal consumer
            configs.remove(ConsumerConfig.GROUP_ID_CONFIG);
            consumer = new KafkaConsumer<>(configs, new ByteArrayDeserializer(), new ByteArrayDeserializer());
        }
        return consumer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized AdminClient getAdminClient() {
        if (adminClient == null) {
            var configs = getAdminClientConfigs(streamsConfig.getConfAsMap());
            var clientId = streamsConfig.getOptionalString(CLIENT_ID_CONFIG).orElse(applicationId());
            var adminClientId = clientId + "-" + containerId + "-admin";
            configs.put(BOOTSTRAP_SERVERS_CONFIG, streamsConfig.getString(BOOTSTRAP_SERVERS_CONFIG));
            configs.put(CLIENT_ID_CONFIG, adminClientId);
            adminClient = AdminClient.create(configs);
        }
        return adminClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(final Duration timeout) {
        close(false, timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
                        return state == State.Standards.STOPPED;
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
                    interceptors.iterator(),
                    (interceptor, chain) -> interceptor.onStop(newStreamsLifecycleContext(), chain),
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
                var oldState = State.Standards.valueOf(kafkaStreams.state().name());
                stateChanges(new StateChangeEvent(State.Standards.STOPPED, oldState));
                // Close all EventStreams
                eventStreams.forEach(EventStream::close);
            }, "kafka-streams-container-close-thread");

            shutdownThread.setDaemon(true);
            shutdownThread.start();
        }

        final long waitMs = timeout.toMillis();
        if (waitMs > 0 && !waitUntilContainerIsStopped(waitMs)) {
            LOG.debug(
                "KafkaStreamsContainer cannot transit to {} within {}ms (id={})",
                ContainerState.STOPPED,
                waitMs,
                applicationId()
            );
        }
    }

    private StreamsLifecycleContext newStreamsLifecycleContext() {
        return new StreamsLifecycleContext() {
            @Override
            public void setState(final State state) {
                LocalKafkaStreamsContainer.this.setState(state);
            }

            @Override
            public KafkaStreamsContainer container() {
                return LocalKafkaStreamsContainer.this;
            }
        };
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

    private boolean waitUntilContainerIsStopped(final long waitMs) {
        final long start = Time.SYSTEM.milliseconds();
        synchronized (containerStateLock) {
            long elapsedMs = 0L;
            while (containerState != ContainerState.STOPPED) {
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

    /**
     * {@inheritDoc}
     */
    @Override
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
        CompletableFuture<State> f = (CompletableFuture<State>) start(executor);
        f.handle((state, throwable) -> {
            if (throwable != null) {
                LOG.error("Unexpected error happens while restarting streams", throwable);
            }
            return state;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LocalStorePartitionLags> allLocalStorePartitionLags() {
        return kafkaStreams.allLocalStorePartitionLags()
            .entrySet()
            .stream()
            .map(entry  -> {
                var storeName = entry.getKey();
                var positions = new ArrayList<PartitionLogOffsetsAndLag>(entry.getValue().size());
                entry.getValue().forEach((partition, lag) -> {
                    var offsetsAndLag = new PartitionLogOffsetsAndLag(
                        partition,
                        lag.currentOffsetPosition(),
                        lag.endOffsetPosition(),
                        lag.offsetLag()
                    );
                    positions.add(offsetsAndLag);
                });
                return new LocalStorePartitionLags(storeName, positions);
            })
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ServerMetadata> localServerMetadata() {
        return allMetadata()
           .stream()
           .filter(ServerMetadata::isLocal)
           .findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ServerMetadata> allMetadata() {
        if (!isRunning()) return Collections.emptySet();

        // allMetadata throw an IllegalAccessException if instance is not running
        return kafkaStreams.allMetadata()
           .stream()
           .map(this::newServerInfoFor)
           .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ServerMetadata> allMetadataForStore(final String storeName) {
        Objects.requireNonNull(storeName, "storeName cannot be null");
        if (!isRunning()) return Collections.emptySet();

        Collection<StreamsMetadata> metadata = kafkaStreams.allMetadataForStore(storeName);
        return metadata.stream()
            .map(this::newServerInfoFor)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K> Optional<KeyQueryMetadata> findMetadataForStoreAndKey(final String storeName,
                                                                      final K key,
                                                                      final Serializer<K> keySerializer) {
        Objects.requireNonNull(storeName, "storeName cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(keySerializer, "keySerializer cannot be null");

        if (!initialized()) return Optional.empty();

        KeyQueryMetadata metadata = kafkaStreams.queryMetadataForKey(storeName, key, keySerializer);
        return metadata == null || metadata.equals(KeyQueryMetadata.NOT_AVAILABLE) ?
            Optional.empty(): Optional.of(metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalStoreAccessor<ReadOnlyKeyValueStore<K, V>> localKeyValueStore(final String store) {
        return getLocalStoreAccess(store, QueryableStoreTypes.keyValueStore());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalStoreAccessor<ReadOnlyKeyValueStore<K, ValueAndTimestamp<V>>> localTimestampedKeyValueStore(
            final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.timestampedKeyValueStore());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalStoreAccessor<ReadOnlyWindowStore<K, V>> localWindowStore(final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.windowStore());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalStoreAccessor<ReadOnlyWindowStore<K, ValueAndTimestamp<V>>> localTimestampedWindowStore(
            final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.timestampedWindowStore());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalStoreAccessor<ReadOnlySessionStore<K, V>> localSessionStore(final String storeName) {
        return getLocalStoreAccess(storeName, QueryableStoreTypes.sessionStore());
    }

    private <T> LocalStoreAccessor<T> getLocalStoreAccess(final String storeName,
                                                          final QueryableStoreType<T> storeType) {

        return new LocalStoreAccessor<>(() -> kafkaStreams.store(fromNameAndType(storeName, storeType)));
    }

    Logger logger() {
        return LOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        if (!initialized()) return false;

        // This is equivalent to the KafkaStreams methods :
        // State.isRunning() <= 2.4 or State.isRunningOrRebalancing() >= 2.5
        final KafkaStreams.State state = kafkaStreams.state();
        return state.equals(KafkaStreams.State.RUNNING) || state.equals(KafkaStreams.State.REBALANCING);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addStateChangeWatcher(final StateChangeWatcher watcher) {
        stateChangeWatchers.add(Objects.requireNonNull(watcher, "Cannot register null watcher"));
    }

    void stateChanges(final StateChangeEvent stateChangeEvent) {
        state = new TimestampedValue<>(stateChangeEvent.timestamp(), stateChangeEvent.newState());
        if (state.value() == State.Standards.RUNNING) {
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

    private ServerMetadata newServerInfoFor(final StreamsMetadata metadata) {
        var hostInfo = new ServerHostInfo(applicationId(), metadata.host(), metadata.port(), isLocal(metadata));
        return new ServerMetadata(
            hostInfo,
            metadata.stateStoreNames(),
            groupByTopicThenGet(metadata.topicPartitions()),
            metadata.standbyStateStoreNames(),
            groupByTopicThenGet(metadata.standbyTopicPartitions())
        );
    }

    private boolean isLocal(final StreamsMetadata metadata) {
        return isSameHost(new HostInfo(metadata.host(), metadata.port()));
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

    private static Map<String, Object> getAdminClientConfigs(final Map<String, Object> configs) {
        return getConfigsForKeys(configs, AdminClientConfig.configNames());
    }

    private static Map<String, Object> getConsumerConfigs(final Map<String, Object> configs) {
        return getConfigsForKeys(configs, ConsumerConfig.configNames());
    }

    private static Map<String, Object> getProducerConfigs(final Map<String, Object> configs) {
        return getConfigsForKeys(configs, ProducerConfig.configNames());
    }

    private static Map<String, Object> getConfigsForKeys(final Map<String, Object> configs,
                                                         final Set<String> keys) {
        final Map<String, Object> parsed = new HashMap<>();
        for (final String configName : keys) {
            if (configs.containsKey(configName)) {
                parsed.put(configName, configs.get(configName));
            }
        }
        return parsed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KafkaStreams getKafkaStreams() {
        validateInitialized();
        return kafkaStreams;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Topology getTopology() {
        return topologyDefinition.getTopology();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSameHost(final HostInfo info) {
        return applicationServer.equals(info.host() + ":" + info.port());
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
            if (adminClient != null) adminClient.close();
        } catch (Exception e) {
            LOG.error("Unexpected error occurred while closing internal resources", e);
        } finally {
            consumer = null;
            adminClient = null;
        }
    }

    /**
     * @return {@code true} if the {@link KafkaStreams} is not equal {@code null},
     *                      i.e container has been started at least once.
     */
    private boolean initialized() {
        return kafkaStreams != null;
    }

}
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
package io.streamthoughts.azkarra.runtime.env;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.State;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.StreamsTopologyExecution;
import io.streamthoughts.azkarra.api.StreamsTopologyMeta;
import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.RocksDBConfig;
import io.streamthoughts.azkarra.api.errors.AlreadyExistsException;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.events.EventStream;
import io.streamthoughts.azkarra.api.events.EventStreamProvider;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.runtime.streams.LocalKafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsFactory;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.streams.errors.StreamThreadExceptionHandler;
import io.streamthoughts.azkarra.api.streams.topology.TopologyDefinition;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.runtime.env.internal.EnvironmentAwareComponentSupplier;
import io.streamthoughts.azkarra.runtime.streams.DefaultApplicationIdBuilder;
import io.streamthoughts.azkarra.runtime.streams.errors.CloseKafkaStreamsOnThreadException;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.StateRestoreListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The default {@link StreamsExecutionEnvironment} implementation.
 */
public class LocalStreamsExecutionEnvironment
        implements StreamsExecutionEnvironment<LocalStreamsExecutionEnvironment>, AzkarraContextAware {

    public final static String TYPE = "local";

    private static final Logger LOG = LoggerFactory.getLogger(LocalStreamsExecutionEnvironment.class);

    /**
     * Static helper that can be used to creates a new {@link StreamsExecutionEnvironment} instance
     * using the empty configuration and a generated unique name.
     *
     * @return a new {@link LocalStreamsExecutionEnvironment} instance.
     */
    public static LocalStreamsExecutionEnvironment create() {
        return create(Conf.empty());
    }

    /**
     * Static helper that can be used to creates a new {@link StreamsExecutionEnvironment} instance from
     * the specified env name and using the configuration.
     *
     * @param envName  the name to be used for identifying this environment.
     *
     * @return a new {@link LocalStreamsExecutionEnvironment} instance.
     */
    public static LocalStreamsExecutionEnvironment create(final String envName) {
        return create(Conf.empty(), envName);
    }

    /**
     * Static helper that can be used to creates a new {@link StreamsExecutionEnvironment} instance from
     * the specified {@link Conf} and using a generated env name.
     *
     * @param settings  the {@link Conf} instance.
     *
     * @return a new {@link LocalStreamsExecutionEnvironment} instance.
     */
    public static LocalStreamsExecutionEnvironment create(final Conf settings) {
        return create(settings, EnvironmentNameGenerator.generate());
    }

    /**
     * Static helper that can be used to creates a new {@link StreamsExecutionEnvironment} instance from
     * the specified {@link Conf} and env name.
     *
     * @param settings  the {@link Conf} instance.
     * @param envName   the name to be used for identifying this environment.
     *
     * @return a new {@link LocalStreamsExecutionEnvironment} instance.
     */
    public static LocalStreamsExecutionEnvironment create(final Conf settings, final String envName) {
        return new LocalStreamsExecutionEnvironment(settings, envName);
    }

    private static final ThreadPerStreamsExecutor STREAMS_EXECUTOR = new ThreadPerStreamsExecutor();

    /**
     * An internal name used to identify this environment.
     */
    private final String name;

    /**
     * The current state of this environment.
     */
    private State state;

    private Conf configuration;

    private final List<KafkaStreams.StateListener> stateListeners = new LinkedList<>();

    private final List<StateRestoreListener> restoreListeners = new LinkedList<>();

    /**
     * The list of topologies to initialize when the environment is started.
     */
    private final List<TopologyDefinitionHolder> topologies;

    /**
     * The list of streams instances currently started.
     */
    private final Map<ApplicationId, KafkaStreamsContainer> activeStreams;

    private AzkarraContext context;

    private Supplier<KafkaStreamsFactory> kafkaStreamsFactory;

    private final List<Supplier<StreamsLifecycleInterceptor>> interceptors;

    private Supplier<StreamThreadExceptionHandler> streamThreadExceptionHandler;

    private Supplier<ApplicationIdBuilder> applicationIdBuilderSupplier;

    private boolean isDefault;

    /**
     * Creates a new {@link LocalStreamsExecutionEnvironment} instance.
     *
     * @param configuration  the default {@link Conf} instance.
     */
    public LocalStreamsExecutionEnvironment(final Conf configuration) {
        this(configuration, EnvironmentNameGenerator.generate());
    }

    /**
     * Creates a new {@link LocalStreamsExecutionEnvironment} instance.
     *
     * @param envName the environment name to be used.
     */
    private LocalStreamsExecutionEnvironment(final Conf config,
                                             final String envName) {
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(envName, "envName cannot be null");
        this.configuration = config;
        this.activeStreams = new HashMap<>();
        this.interceptors = new LinkedList<>();
        this.kafkaStreamsFactory = () -> KafkaStreamsFactory.DEFAULT;
        this.applicationIdBuilderSupplier = DefaultApplicationIdBuilder::new;
        this.topologies = new LinkedList<>();
        this.name = envName;
        setState(State.CREATED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public State state() {
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public boolean isDefault(final boolean isDefault) {
        return this.isDefault = isDefault;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsTopologyExecution newTopologyExecution(final StreamsTopologyMeta meta, final Executed executed) {
        return new LocalStreamsExecution(meta, executed, context, this);
    }

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
    public LocalStreamsExecutionEnvironment addStateListener(final KafkaStreams.StateListener listener) {
        Objects.requireNonNull(listener, "Cannot add empty listener");
        stateListeners.add(listener);
        return this;
    }

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
    public LocalStreamsExecutionEnvironment addGlobalStateListener(final StateRestoreListener listener) {
        Objects.requireNonNull(listener, "Cannot add empty listener");
        restoreListeners.add(listener);
        return this;
    }

    /**
     * Adds a streams interceptor that will set to all {@link KafkaStreams} instance created
     * in this {@link StreamsExecutionEnvironment}.
     * The interceptors will be executed in the order in which they were added.
     *
     * @param interceptor   the {@link {@link StreamsLifecycleInterceptor}}.
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    public LocalStreamsExecutionEnvironment addStreamsLifecycleInterceptor(
            final Supplier<StreamsLifecycleInterceptor> interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    /**
     * Sets the {@link StreamThreadExceptionHandler} invoked when a StreamThread abruptly terminates
     * due to an uncaught exception.
     *
     * @param handler   the {@link StreamThreadExceptionHandler}.
     * @return          this {@link StreamsExecutionEnvironment} instance.
     *
     * @see KafkaStreams#setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)
     */
    public LocalStreamsExecutionEnvironment setStreamThreadExceptionHandler(
            final Supplier<StreamThreadExceptionHandler> handler) {
        streamThreadExceptionHandler = Objects.requireNonNull(handler, "handle cannot be null");
        return this;
    }

    /**
     * Gets the {@link StreamThreadExceptionHandler}.
     *
     * @return          the {@link Supplier<StreamThreadExceptionHandler>}, otherwise {@code null} if no handler is set.
     */
    public Supplier<StreamThreadExceptionHandler> getStreamThreadExceptionHandler() {
        return streamThreadExceptionHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<KafkaStreamsContainer> applications() {
        return activeStreams.values();
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public Set<String> applicationIds() {
        return activeStreams.keySet().stream().map(ApplicationId::toString).collect(Collectors.toSet());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LocalStreamsExecutionEnvironment setConfiguration(final Conf configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalStreamsExecutionEnvironment setRocksDBConfig(final RocksDBConfig rocksDBConfig) {
        Objects.requireNonNull(rocksDBConfig, "rocksDBConfig cannot be null");
        configuration = configuration.withFallback(Conf.of("streams", rocksDBConfig.conf()));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalStreamsExecutionEnvironment setApplicationIdBuilder(final Supplier<ApplicationIdBuilder> supplier) {
        Objects.requireNonNull(supplier, "builder cannot be null");
        applicationIdBuilderSupplier = supplier;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Supplier<ApplicationIdBuilder> getApplicationIdBuilder() {
        return applicationIdBuilderSupplier;
    }

    /**
     * Add a new {@link TopologyProvider} instance to this {@link StreamsExecutionEnvironment} to be started.
     *
     * @param provider     the {@link TopologyProvider} supplier.
     *
     * @return             this {@link ApplicationId} instance if the environment is already started,
     *                     otherwise {@code null}.
     */
    public ApplicationId addTopology(final Supplier<TopologyProvider> provider) {
        return addTopology(provider, new InternalExecuted());
    }

    /**
     * Add a new {@link TopologyProvider} instance to this {@link StreamsExecutionEnvironment} to be started.
     *
     * @param provider     the {@link TopologyProvider} supplier.
     * @param executed     the {@link Executed} instance.
     *
     * @return             this {@link ApplicationId} instance if the environment is already started,
     *                     otherwise {@code null}.
     */
    public ApplicationId addTopology(final Supplier<TopologyProvider> provider, final Executed executed) {
        final TopologyDefinitionHolder internalProvider = new TopologyDefinitionHolder(provider, executed);
        topologies.add(internalProvider);
        return state == State.STARTED ? start(internalProvider) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws IllegalStateException, AzkarraException {
        if (state != State.CREATED) {
            throw new IllegalStateException(
                "The environment is either already started or already stopped, cannot re-start");
        }
        topologies.forEach(this::start);
        setState(State.STARTED);
    }

    private ApplicationId start(final TopologyDefinitionHolder topologyHolder) {
        final TopologyDefinition definition = topologyHolder.createTopologyDefinition();

        LOG.info("Building new Topology for name='{}', version='{}'", definition.getName(), definition.getVersion());

        var topologyConfig = topologyHolder.getTopologyConfig();

        var applicationId = generateApplicationId(definition, topologyHolder.getTopologyConfig());
        checkStreamsIsAlreadyRunningFor(applicationId);

        topologyHolder.setApplicationId(applicationId);

        var streamsConfig = topologyConfig.hasPath("streams") ? topologyConfig.getSubConf("streams") : Conf.empty();
        var applicationIdConfig = Conf.of(StreamsConfig.APPLICATION_ID_CONFIG, applicationId.toString());

        if (streamThreadExceptionHandler == null)
            streamThreadExceptionHandler = CloseKafkaStreamsOnThreadException::new;

        var threadExceptionHandler = supply(streamThreadExceptionHandler, topologyConfig);

        var kafkaStreamsContainer = LocalKafkaStreamsContainer.newBuilder()
            .withStateListeners(stateListeners)
            .withRestoreListeners(restoreListeners)
            .withStreamThreadExceptionHandlers(List.of(threadExceptionHandler))
            .withStreamsConfig(Conf.of(applicationIdConfig, streamsConfig))
            .withTopologyDefinition(definition)
            .withKafkaStreamsFactory(topologyHolder.getKafkaStreamsFactory())
            .withInterceptors(topologyHolder.getAllInterceptors())
            .build();

        activeStreams.put(applicationId, kafkaStreamsContainer);
        kafkaStreamsContainer.start(STREAMS_EXECUTOR);

        return applicationId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final boolean cleanUp) {
        LOG.info("Stopping streams environment '{}'", name);
        checkIsStarted();
        try {
            for (final ApplicationId id : activeStreams.keySet()) {
                try {
                    stop(id, cleanUp);
                } catch (IllegalStateException e) {
                    LOG.warn(e.getMessage());
                }
            }
        } catch (final Exception e) {
            LOG.error("Error happens while stopping Kafka Streams instance.", e);
        } finally {
            setState(State.STOPPED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final ApplicationId id, final boolean cleanUp, final Duration timeout) {
        checkIsStarted();
        closeStreamsContainer(id, cleanUp, timeout, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final ApplicationId id, final Duration timeout) {
        checkIsStarted();
        closeStreamsContainer(id, true, timeout, true);
    }

    /**
     * Close the {@link KafkaStreams} instance for the given identifier and wait up to the {@code timeout}
     * for the instance to be closed.
     *
     * @param id        the streams application identifier.
     * @param cleanUp   flag to indicate if local states must be cleanup.
     * @param timeout   the duration to wait for the streams to shutdown.
     * @param remove    if the instance should be removed from active streams.
     *
     * @throws IllegalArgumentException if no streams instance exist for the given {@code id}.
     */
    private void closeStreamsContainer(final ApplicationId id,
                                       final boolean cleanUp,
                                       final Duration timeout,
                                       final boolean remove) {
        KafkaStreamsContainer container = activeStreams.get(id);
        if (container == null) {
            throw new IllegalStateException("Try to stop a non existing streams applications.");
        }
        container.close(cleanUp, timeout);
        if (remove) {
            activeStreams.remove(id);
            topologies.removeIf(t -> t.matches(id));
            LOG.info("Streams instance '{}' was removed from environment '{}'", id, name);
        }
    }

    private void setState(final State started) {
        state = started;
    }

    private void checkStreamsIsAlreadyRunningFor(final ApplicationId id) {
        if (activeStreams.containsKey(id)) {
            throw new AlreadyExistsException(
                "A streams instance is already registered for application.id '" + id + "'");
        }
    }

    private void checkIsStarted() {
        if (state != State.STARTED) {
            throw new IllegalStateException("Environment is not started. State is " + state + ".");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalStreamsExecutionEnvironment addFallbackConfiguration(final Conf fallback) {
        configuration = configuration.withFallback(fallback);
        return this;
    }

    /**
     * Sets the {@link KafkaStreamsFactory} that will be used to provide
     * the {@link KafkaStreams} to configure and start.
     *
     * @param factory   the {@link KafkaStreamsFactory} instance.
     * @return this {@link StreamsExecutionEnvironment} instance.
     */
    public LocalStreamsExecutionEnvironment setKafkaStreamsFactory(final Supplier<KafkaStreamsFactory> factory) {
        this.kafkaStreamsFactory = factory;
        return this;
    }

    private ApplicationId generateApplicationId(final TopologyDefinition definition,
                                                final Conf TopologyConfig) {
        var applicationIdBuilder = supply(applicationIdBuilderSupplier, TopologyConfig);
        return applicationIdBuilder.buildApplicationId(
            new TopologyMetadata(definition.getName(), definition.getVersion(), definition.getDescription()),
            TopologyConfig
        );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setAzkarraContext(final AzkarraContext context) {
        this.context = context;
    }

    private static final class EnvironmentNameGenerator {

        private static final AtomicInteger NUM = new AtomicInteger(1);

        static String generate() {
            return String.format("__streams_env_%02d", NUM.getAndIncrement());
        }
    }

    /**
     * Inner {@link Executor} which is used for starting {@link KafkaStreams} instance.
     * One new {@link Thread} is created per streams instance.
     */
    private static final class ThreadPerStreamsExecutor implements Executor {

        private static final AtomicInteger COUNTER = new AtomicInteger();

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final Runnable r) {
            final Thread thread = new Thread(r, threadName());
            // Ensure thread is start as non-daemon - Kafka StreamsThread will inherit from parent this one.
            thread.setDaemon(false);
            thread.start();
        }

        private String threadName() {
            return "streams-starter-" + COUNTER.incrementAndGet();
        }
    }

    @VisibleForTesting
    class TopologyDefinitionHolder {

        private final Supplier<TopologyProvider> supplier;
        private final InternalExecuted executed;
        private ApplicationId applicationId;

        /**
         * Creates a new {@link TopologyDefinitionHolder} instance.
         *
         * @param supplier  the supplier to supplier.
         * @param executed  the {@link Executed} instance.
         */
        TopologyDefinitionHolder(final Supplier<TopologyProvider> supplier,
                                 final Executed executed) {
            this.supplier = supplier;
            this.executed = new InternalExecuted(executed);
        }

        public void setApplicationId(final ApplicationId applicationId) {
            this.applicationId = applicationId;
        }

        List<StreamsLifecycleInterceptor> getAllInterceptors() {
            return Stream
                .concat(interceptors.stream(), executed.interceptors().stream())
                .map(i -> supply(i, getTopologyConfig()))
                .collect(Collectors.toList());
        }

        KafkaStreamsFactory getKafkaStreamsFactory() {
            final Supplier<KafkaStreamsFactory> factory = executed.factory().orElse(kafkaStreamsFactory);
            return supply(factory, getTopologyConfig());
        }

        Conf getTopologyConfig() {
            var ctxConfig = context != null ? context.getConfiguration() : Conf.empty();
            var envConfig = LocalStreamsExecutionEnvironment.this.getConfiguration();
            // Merged all configurations
            return Conf.of(executed.config(), envConfig, envConfig, ctxConfig);
        }

        TopologyDefinition createTopologyDefinition() {
            return new InternalTopologyDefinition(
                executed.name(),
                executed.description(),
                supply(supplier, getTopologyConfig())
            );
        }

        boolean matches(final ApplicationId applicationId) {
            return this.applicationId.equals(applicationId);
        }
    }

    private <T> T supply(final Supplier<T> supplier, final Conf componentConfig) {
        return new EnvironmentAwareComponentSupplier<>(supplier).get(this, componentConfig);
    }


    private static class InternalTopologyDefinition implements TopologyDefinition {

        private final String name;
        private final String description;
        private final TopologyProvider provider;

        private final Topology topology;

        InternalTopologyDefinition(final String name,
                                   final String description,
                                   final TopologyProvider provider) {
            this.name = name;
            this.description = description;
            this.provider = provider;
            this.topology = provider.topology();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getVersion() {
            return provider.version();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return description;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Topology getTopology() {
            return topology;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<EventStream> getEventStreams() {
            return (provider instanceof EventStreamProvider) ?
                ((EventStreamProvider)provider).eventStreams() :
                Collections.emptyList();
        }
    }
}
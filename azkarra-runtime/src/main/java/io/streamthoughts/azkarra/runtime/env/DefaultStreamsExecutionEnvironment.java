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
package io.streamthoughts.azkarra.runtime.env;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.State;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.RocksDBConfig;
import io.streamthoughts.azkarra.api.errors.AlreadyExistsException;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.KafkaStreamContainerBuilder;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsFactory;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.streams.errors.StreamThreadExceptionHandler;
import io.streamthoughts.azkarra.api.streams.topology.TopologyContainer;
import io.streamthoughts.azkarra.runtime.env.internal.EnvironmentAwareComponentSupplier;
import io.streamthoughts.azkarra.runtime.env.internal.TopologyContainerFactory;
import io.streamthoughts.azkarra.runtime.interceptors.WaitForSourceTopicsInterceptor;
import io.streamthoughts.azkarra.runtime.streams.DefaultApplicationIdBuilder;
import io.streamthoughts.azkarra.runtime.streams.errors.CloseKafkaStreamsOnThreadException;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import org.apache.kafka.streams.KafkaStreams;
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
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The default {@link StreamsExecutionEnvironment} implementation.
 */
public class DefaultStreamsExecutionEnvironment implements StreamsExecutionEnvironment, AzkarraContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultStreamsExecutionEnvironment.class);

    /**
     * Static helper that can be used to creates a new {@link StreamsExecutionEnvironment} instance
     * using the empty configuration and a generated unique name.
     *
     * @return a new {@link StreamsExecutionEnvironment} instance.
     */
    public static StreamsExecutionEnvironment create() {
        return create(Conf.empty());
    }

    /**
     * Static helper that can be used to creates a new {@link StreamsExecutionEnvironment} instance from
     * the specified env name and using the configuration.
     *
     * @param envName  the name to be used for identifying this environment.
     *
     * @return a new {@link StreamsExecutionEnvironment} instance.
     */
    public static StreamsExecutionEnvironment create(final String envName) {
        return create(Conf.empty(), envName);
    }

    /**
     * Static helper that can be used to creates a new {@link StreamsExecutionEnvironment} instance from
     * the specified {@link Conf} and using a generated env name.
     *
     * @param settings  the {@link Conf} instance.
     *
     * @return a new {@link StreamsExecutionEnvironment} instance.
     */
    public static StreamsExecutionEnvironment create(final Conf settings) {
        return create(settings, EnvironmentNameGenerator.generate());
    }

    /**
     * Static helper that can be used to creates a new {@link StreamsExecutionEnvironment} instance from
     * the specified {@link Conf} and env name.
     *
     * @param settings  the {@link Conf} instance.
     * @param envName   the name to be used for identifying this environment.
     *
     * @return a new {@link StreamsExecutionEnvironment} instance.
     */
    public static StreamsExecutionEnvironment create(final Conf settings, final String envName) {
        return new DefaultStreamsExecutionEnvironment(settings, envName);
    }

    private static ThreadPerStreamsExecutor STREAMS_EXECUTOR = new ThreadPerStreamsExecutor();

    /**
     * An internal name used to identify this environment.
     */
    private final String name;

    /**
     * The current state of this environment.
     */
    private State state;

    private Conf configuration;

    private List<KafkaStreams.StateListener> stateListeners = new LinkedList<>();

    private List<StateRestoreListener> restoreListeners = new LinkedList<>();

    private final TopologyContainerFactory topologyFactory;

    /**
     * The list of topologies to initialize when the environment is started.
     */
    private final List<InternalTopologyProvider> topologies;

    /**
     * The list of streams instances currently started.
     */
    private final Map<ApplicationId, KafkaStreamsContainer> activeStreams;

    private AzkarraContext context;

    private Supplier<KafkaStreamsFactory> kafkaStreamsFactory;

    private final List<Supplier<StreamsLifecycleInterceptor>> interceptors;

    private boolean waitForTopicToBeCreated = false;

    private Supplier<StreamThreadExceptionHandler> streamThreadExceptionHandler;

    /**
     * Creates a new {@link DefaultStreamsExecutionEnvironment} instance.
     *
     * @param configuration  the default {@link Conf} instance.
     */
    public DefaultStreamsExecutionEnvironment(final Conf configuration) {
        this(configuration, EnvironmentNameGenerator.generate());
    }

    /**
     * Creates a new {@link DefaultStreamsExecutionEnvironment} instance.
     *
     * @param envName the environment name to be used.
     */
    private DefaultStreamsExecutionEnvironment(final Conf config,
                                               final String envName) {
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(envName, "envName cannot be null");
        this.configuration = config;
        this.activeStreams = new HashMap<>();
        this.interceptors = new LinkedList<>();
        this.kafkaStreamsFactory = () -> KafkaStreamsFactory.DEFAULT;
        this.topologyFactory = new TopologyContainerFactory(this, DefaultApplicationIdBuilder::new);
        this.topologies = new LinkedList<>();
        this.name = envName;
        setState(State.CREATED);
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
    public StreamsExecutionEnvironment addStateListener(final KafkaStreams.StateListener listener) {
        Objects.requireNonNull(listener, "Cannot add empty listener");
        stateListeners.add(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment addGlobalStateListener(final StateRestoreListener listener) {
        Objects.requireNonNull(listener, "Cannot add empty listener");
        restoreListeners.add(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment addStreamsLifecycleInterceptor(
            final Supplier<StreamsLifecycleInterceptor> interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment setStreamThreadExceptionHandler(
            final Supplier<StreamThreadExceptionHandler> handler) {
        streamThreadExceptionHandler = Objects.requireNonNull(handler, "handle cannot be null");
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     */
    @Override
    public StreamsExecutionEnvironment setConfiguration(final Conf configuration) {
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
    public StreamsExecutionEnvironment setRocksDBConfig(final RocksDBConfig rocksDBConfig) {
        Objects.requireNonNull(rocksDBConfig, "rocksDBConfig cannot be null");
        configuration = configuration.withFallback(Conf.with("streams", rocksDBConfig.conf()));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment setApplicationIdBuilder(final Supplier<ApplicationIdBuilder> supplier) {
        Objects.requireNonNull(supplier, "builder cannot be null");
        topologyFactory.setApplicationIdBuilder(supplier);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Supplier<ApplicationIdBuilder> getApplicationIdBuilder() {
        return topologyFactory.getApplicationIdBuilderSupplier();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment setWaitForTopicsToBeCreated(boolean waitForTopicToBeCreated) {
        this.waitForTopicToBeCreated = waitForTopicToBeCreated;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId addTopology(final Supplier<TopologyProvider> provider) {
        return addTopology(provider, new InternalExecuted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId addTopology(final Supplier<TopologyProvider> provider, final Executed executed) {
        final InternalTopologyProvider internalProvider = new InternalTopologyProvider(provider, executed);
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

    private ApplicationId start(final InternalTopologyProvider topologyProvider) {

        final TopologyContainer topologyContainer = topologyProvider.getTopology();

        final ApplicationId applicationId = topologyContainer.applicationId();
        checkStreamsIsAlreadyRunningFor(applicationId);

        if (streamThreadExceptionHandler == null)
            streamThreadExceptionHandler = CloseKafkaStreamsOnThreadException::new;

        final Thread.UncaughtExceptionHandler exceptionHandler = new InternalKafkaStreamsUncaughtExceptionHandler(
            applicationId,
            new EnvironmentAwareComponentSupplier<>(streamThreadExceptionHandler)
                .get(this, topologyContainer.streamsConfig())
        );
        final KafkaStreamsContainer streamsContainer = KafkaStreamContainerBuilder.newBuilder()
            .withTopologyContainer(topologyContainer)
            .withStateListeners(stateListeners)
            .withRestoreListeners(restoreListeners)
            .withUncaughtExceptionHandler(Collections.singletonList(exceptionHandler))
            .withKafkaStreamsFactory(topologyProvider.getKafkaStreamsFactory())
            .build();

        activeStreams.put(applicationId, streamsContainer);

        streamsContainer.start(STREAMS_EXECUTOR);

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
            topologies.removeIf(t -> t.isApplication(id));
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
    public StreamsExecutionEnvironment addFallbackConfiguration(final Conf fallback) {
        configuration = configuration.withFallback(fallback);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment setKafkaStreamsFactory(final Supplier<KafkaStreamsFactory> kafkaStreamsFactory) {
        this.kafkaStreamsFactory = kafkaStreamsFactory;
        return this;
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
    class InternalTopologyProvider {

        private final Supplier<TopologyProvider> supplier;
        private final InternalExecuted executed;

        private TopologyContainer container;

        /**
         * Creates a new {@link InternalTopologyProvider} instance.
         *
         * @param supplier  the supplier to supplier.
         * @param executed  the {@link Executed} instance.
         */
        InternalTopologyProvider(final Supplier<TopologyProvider> supplier,
                                 final Executed executed) {
            this.supplier = supplier;
            this.executed = new InternalExecuted(executed);
        }

        public TopologyContainer getTopology() {

            if (container == null) {
                final Conf topologyConfig = getTopologyConfig();

                // Merged all interceptors
                final List<Supplier<StreamsLifecycleInterceptor>> allInterceptors =
                        Stream.concat(
                            interceptors.stream(),
                            executed.interceptors().stream()
                        ).collect(Collectors.toList());

                if (waitForTopicToBeCreated)
                    allInterceptors.add(WaitForSourceTopicsInterceptor::new);

                final Executed merged = new InternalExecuted()
                    .withName(executed.name())
                    .withDescription(executed.description())
                    .withConfig(topologyConfig)
                    .withInterceptors(allInterceptors);
                container = topologyFactory.make(supplier, merged);
            }
            return container;
        }

        boolean isApplication(final ApplicationId id) {
            return container.applicationId().equals(id);
        }

        private KafkaStreamsFactory getKafkaStreamsFactory() {
            final Supplier<KafkaStreamsFactory> factory = executed.factory().orElse(kafkaStreamsFactory);
            return new EnvironmentAwareComponentSupplier<>(factory).get(
                DefaultStreamsExecutionEnvironment.this,
                getTopologyConfig()
            );
        }

        private Conf getTopologyConfig() {
            // Merged all configurations
            return executed.config()
                .withFallback(DefaultStreamsExecutionEnvironment.this.getConfiguration())
                .withFallback(context != null ? context.getConfiguration() : Conf.empty());
        }
    }

    private class InternalKafkaStreamsUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private final ApplicationId id;
        private final StreamThreadExceptionHandler delegate;

        InternalKafkaStreamsUncaughtExceptionHandler(final ApplicationId id,
                                                     final StreamThreadExceptionHandler handler) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
            this.delegate = Objects.requireNonNull(handler, "handler cannot be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            LOG.debug("Catch uncaught exception, executing handler: {}", delegate.getClass());
            final KafkaStreamsContainer container = activeStreams.get(id);
            delegate.handle(container, t, e);
        }
    }
}
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
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentAware;
import io.streamthoughts.azkarra.api.StreamsLifeCycleInterceptor;
import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.config.RocksDBConfig;
import io.streamthoughts.azkarra.api.errors.AlreadyExistsException;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.KafkaStreamContainerBuilder;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.streams.topology.TopologyContainer;
import io.streamthoughts.azkarra.runtime.streams.DefaultApplicationIdBuilder;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import io.streamthoughts.azkarra.runtime.streams.topology.TopologyFactory;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.processor.StateRestoreListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private boolean waitForTopicToBeCreated = false;

    private Conf configuration;

    private List<KafkaStreams.StateListener> stateListeners = new LinkedList<>();

    private List<StateRestoreListener> restoreListeners = new LinkedList<>();

    private final TopologyFactory topologyFactory;

    private Supplier<ApplicationIdBuilder> applicationIdBuilderSupplier;

    private final List<InternalTopologySupplier> topologies;

    private final Map<ApplicationId, KafkaStreamsContainer> streams;

    private AzkarraContext context;

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
        this.streams = new HashMap<>();
        this.topologyFactory = new TopologyFactory(this);
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
    public Collection<KafkaStreamsContainer> applications() {
        return streams.values();
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
        applicationIdBuilderSupplier = supplier;
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
        final InternalTopologySupplier supplier = new InternalTopologySupplier(provider, executed);
        topologies.add(supplier);
        if (state == State.STARTED) {
            Tuple<ApplicationId, TopologyContainer> t = supplier.get();
            start(buildStreamsInstance(t.left(), t.right(), supplier.executed()));
            return t.left();
        }
        return null;
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
        buildAllStreamsInstances();
        streams.values().forEach(this::start);
        setState(State.STARTED);
    }

    public void setState(State started) {
        state = started;
    }

    private void buildAllStreamsInstances() {
        topologies.forEach(supplier -> {
            Tuple<ApplicationId, TopologyContainer> t = supplier.get();
            buildStreamsInstance(t.left(), t.right(), supplier.executed());
        });
    }

    private KafkaStreamsContainer buildStreamsInstance(final ApplicationId id,
                                                       final TopologyContainer topology,
                                                       final InternalExecuted executed) {
        LOG.info("Building new streams instance with name='{}', version='{}', id='{}'.",
            topology.getMetadata().name(),
            topology.getMetadata().version(),
            id);

        checkStreamsIsAlreadyRunningFor(id);

        final List<StreamsLifeCycleInterceptor> interceptors = new LinkedList<>();

        executed.interceptors().forEach(supplier -> {
            final StreamsLifeCycleInterceptor interceptor = supplier.get();
            Configurable.mayConfigure(interceptor, getContextAwareConfig());
            interceptors.add(interceptor);
        });

        interceptors.forEach(i -> LOG.info("Adding streams interceptor: {}", i.getClass().getSimpleName()));

        final KafkaStreamsContainer container = KafkaStreamContainerBuilder.newBuilder()
            .withApplicationId(id)
            .withTopologyContainer(topology)
            .withStateListeners(stateListeners)
            .withRestoreListeners(restoreListeners)
            .withUncaughtExceptionHandler(Collections.singletonList((t, e) -> stop(id, false)))
            .withInterceptors(interceptors)
            .build();

        streams.put(id, container);
        return container;
    }

    private void checkStreamsIsAlreadyRunningFor(final ApplicationId id) {
        if (streams.containsKey(id)) {
            throw new AlreadyExistsException(
                "A streams instance is already registered for application.id '" + id + "'");
        }
    }

    private void start(final KafkaStreamsContainer streams) {
        streams.start(STREAMS_EXECUTOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final boolean cleanUp) {
        LOG.info("Stopping streams environment '{}'", name);
        checkIsStarted();
        try {
            for (final ApplicationId id : streams.keySet()) {
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
    public void stop(final ApplicationId id, final boolean cleanUp) {
        checkIsStarted();
        stop(streams.get(id), cleanUp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final ApplicationId id) {
        checkIsStarted();
        stop(streams.remove(id), true);
        topologies.removeIf(t -> t.applicationId().equals(id));
    }

    private void stop(final KafkaStreamsContainer container, final boolean cleanUp) {
        if (container == null) {
            throw new IllegalStateException("Try to stop a non existing streams applications.");
        }
        container.close(cleanUp);
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
    public void setAzkarraContext(final AzkarraContext context) {
        this.context = context;
    }

    private void maySetStreamsExecutionEnvironmentAware(final Object o) {
        if (StreamsExecutionEnvironmentAware.class.isAssignableFrom(o.getClass())) {
            ((StreamsExecutionEnvironmentAware)o)
                .setExecutionEnvironment(DefaultStreamsExecutionEnvironment.this);
        }
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
    class InternalTopologySupplier implements Supplier<Tuple<ApplicationId, TopologyContainer>> {

        private final Supplier<TopologyProvider> supplier;
        private final InternalExecuted executed;

        private ApplicationId id;

        /**
         * Creates a new {@link InternalTopologySupplier} instance.
         *
         * @param supplier  the supplier to supplier.
         * @param executed  the {@link Executed} instance.
         */
        InternalTopologySupplier(final Supplier<TopologyProvider> supplier,
                                 final Executed executed) {
            this.supplier = supplier;
            this.executed = new InternalExecuted(executed);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Tuple<ApplicationId, TopologyContainer> get() {
            TopologyContainer container = buildTopology(supplier, executed);
            this.id = buildApplicationId(container);
            return Tuple.of(buildApplicationId(container), container);
        }

        public InternalExecuted executed() {
            return executed;
        }

        public ApplicationId applicationId() {
            if (id == null) {
                throw new IllegalStateException(
                    "The application id is not available, the topology has not been built yet");
            }
            return id;
        }

        private TopologyContainer buildTopology(final Supplier<TopologyProvider> supplier,
                                                final Executed executed) {
            Conf defaultConf = getContextAwareConfig();
            return topologyFactory.make(supplier.get(), defaultConf, executed);
        }

        private ApplicationId buildApplicationId(final TopologyContainer container) {
            if (applicationIdBuilderSupplier == null) {
                applicationIdBuilderSupplier = DefaultApplicationIdBuilder::new;
            }
            final ApplicationIdBuilder builder = applicationIdBuilderSupplier.get();
            maySetStreamsExecutionEnvironmentAware(builder);
            return builder.buildApplicationId(container.getMetadata());
        }
    }

    private Conf getContextAwareConfig() {
        Conf conf = configuration;
        // The environment must automatically inherit from context configuration.
        if (context != null) {
            conf = conf.withFallback(context.getConfiguration());
        }
        return conf;
    }
}

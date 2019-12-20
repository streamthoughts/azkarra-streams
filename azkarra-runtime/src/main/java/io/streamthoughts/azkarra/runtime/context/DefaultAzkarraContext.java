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
package io.streamthoughts.azkarra.runtime.context;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.AzkarraContextListener;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.State;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.components.ComponentClassReader;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.components.ContextAwareComponentRegistry;
import io.streamthoughts.azkarra.api.components.Scoped;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.AlreadyExistsException;
import io.streamthoughts.azkarra.api.errors.AzkarraContextException;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsFactory;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentRegistry;
import io.streamthoughts.azkarra.runtime.components.DefaultProviderClassReader;
import io.streamthoughts.azkarra.runtime.components.TopologyDescriptorFactory;
import io.streamthoughts.azkarra.runtime.config.AzkarraContextConfig;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareComponentSupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareKafkaStreamsFactorySupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareLifecycleInterceptorSupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareTopologySupplier;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import io.streamthoughts.azkarra.runtime.interceptors.AutoCreateTopicsInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.ClassloadingIsolationInterceptor;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import io.streamthoughts.azkarra.runtime.util.ShutdownHook;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The AzkarraContext.
 */
public class DefaultAzkarraContext implements AzkarraContext {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAzkarraContext.class);

    public static final String DEFAULT_ENV_NAME = "__default";

    private static final Scoped APPLICATION_SCOPE = Scoped.application();

    /**
     * Static helper that can be used to creates a new {@link AzkarraContext} instance
     * using a default {@link ComponentRegistry} and a empty configuration.
     *
     * @return a new {@link AzkarraContext} instance.
     */
    public static AzkarraContext create() {
        return create(Conf.empty());
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraContext} instance
     * using the specified {@link ComponentRegistry} and a empty configuration.
     *
     * @return a new {@link AzkarraContext} instance.
     */
    public static AzkarraContext create(final ComponentRegistry registry) {
        return create(Conf.empty()).setComponentRegistry(registry);
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraContext} instance
     * using the specified {@link ComponentRegistry} and a empty configuration.
     *
     * @return a new {@link AzkarraContext} instance.
     */
    public static AzkarraContext create(final ComponentClassReader reader) {
        return create(Conf.empty()).setComponentClassReader(reader);
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraContext} instance
     * using a default {@link ComponentRegistry} and the specified configuration.
     *
     * @return a new {@link AzkarraContext} instance.
     */
    public static AzkarraContext create(final Conf configuration) {
        // Set all default implementations
        DefaultProviderClassReader reader = new DefaultProviderClassReader();
        reader.addDescriptorFactoryForType(TopologyProvider.class, new TopologyDescriptorFactory());

        return new DefaultAzkarraContext(configuration)
                .setComponentClassReader(reader)
                .setComponentRegistry(new DefaultComponentRegistry());
    }

    private boolean registerShutdownHook;

    private StreamsExecutionEnvironment defaultEnvironment;

    private Map<String, StreamsExecutionEnvironment> environments;

    private ComponentRegistry registry;

    private final List<AzkarraContextListener> listeners;

    private ComponentClassReader reader;

    private State state;

    private AzkarraContextConfig contextConfig;

    private List<Supplier<StreamsLifecycleInterceptor>> globalInterceptors;

    private Supplier<KafkaStreamsFactory> globalKafkaStreamsFactory;

    /**
     * Creates a new {@link DefaultAzkarraContext} instance.
     *
     * @param configuration   the context {@link Conf} instance.
     */
    private DefaultAzkarraContext(final Conf configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        this.environments = new LinkedHashMap<>();
        this.listeners = new ArrayList<>();
        this.contextConfig = new AzkarraContextConfig(configuration);
        setState(State.CREATED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext setComponentRegistry(final ComponentRegistry registry) {
        Objects.requireNonNull(registry, "registry cannot be null");
        this.registry = new ContextAwareComponentRegistry(this, registry);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentRegistry getComponentRegistry() {
        return registry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentClassReader getComponentClassReader() {
        return reader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext setComponentClassReader(final ComponentClassReader reader) {
        this.reader = reader;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext addListener(final AzkarraContextListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext setRegisterShutdownHook(final boolean registerShutdownHook) {
        this.registerShutdownHook = registerShutdownHook;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getConfiguration() {
        return contextConfig.configs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext setConfiguration(final Conf configuration) {
        this.contextConfig = new AzkarraContextConfig(configuration);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext addConfiguration(final Conf configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        this.contextConfig.addConfiguration(configuration);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext addExecutionEnvironment(final StreamsExecutionEnvironment env)
            throws AlreadyExistsException {
        Objects.requireNonNull(env, "env cannot be null.");
        LOG.info("Creating new streams environment for name '{}'", env.name());
        if (!environments.containsKey(env.name())) {
            environments.put(env.name(), env);
            Map<String, Object> confAsMap = new TreeMap<>(env.getConfiguration().getConfAsMap());
            final String configLogs = confAsMap.entrySet()
                .stream()
                .map(e -> e.getKey() + " = " + e.getValue())
                .collect(Collectors.joining("\n\t"));
            LOG.info("Registered new streams environment for name '{}' and default config :\n\t{}",
                env.name(),
                configLogs
            );
            if (env instanceof AzkarraContextAware)
                ((AzkarraContextAware)env).setAzkarraContext(this);

            if (env.name().equals(DEFAULT_ENV_NAME))
                defaultEnvironment = env;

            if (state == State.STARTED) {
                initializeEnvironment(env);
                env.start();
            }
        } else {
            throw new AlreadyExistsException("Environment already registered for name '" + env.name() + "'");
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId addTopology(final String type, final Executed executed) {
        return addTopology(type, DEFAULT_ENV_NAME, executed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId addTopology(final Class<? extends TopologyProvider> type, final Executed executed) {
        return addTopology(type, DEFAULT_ENV_NAME, executed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId addTopology(final Class<? extends TopologyProvider> type,
                                     final String envName,
                                     final Executed executed) {
        return addTopology(type.getName(), envName, executed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId addTopology(final String type,
                                     final String environment,
                                     final Executed executed) {
        return addTopology(type, null, environment, executed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId addTopology(final String type,
                                     final String version,
                                     final String environment,
                                     final Executed executed) {
        initializeDefaultEnvironment();
        checkIfEnvironmentExists(environment, String.format(
            "Error while adding topology '%s', environment '%s'not found", type, environment
        ));

        if (!registry.isRegistered(type)) {
            try {
                addComponent(type); // This can fail if the specified type is an alias.
            } catch (final AzkarraException e) {
                /* can ignore this exception for now, initialization will fail later */
            }
        }
        return mayAddTopologyToEnvironment(type, version, environment, executed);
    }

    public void setState(final State started) {
        state = started;
    }

    private ApplicationId mayAddTopologyToEnvironment(final String type,
                                                      final String version,
                                                      final String environmentName,
                                                      final Executed executed) {
        final StreamsExecutionEnvironment env = environments.get(environmentName);

        final Optional<ComponentDescriptor<TopologyProvider>> opt = version == null ?
            registry.findLatestDescriptorByAlias(type) :
            registry.findLatestDescriptorByAliasAndVersion(type, version);

        if (opt.isPresent()) {
            final TopologyDescriptor descriptor = (TopologyDescriptor) opt.get();
            return addTopologyToEnvironment(descriptor, env, new InternalExecuted(executed));

        } else {
            final String loggedVersion = version != null ? version : "latest";
            throw new AzkarraContextException(
               "Failed to register topology to environment '" + environmentName + "'." +
                " Cannot find any topology provider for type='" + type + "', version='" + loggedVersion +" '."
            );
        }
    }

    private ApplicationId addTopologyToEnvironment(final TopologyDescriptor descriptor,
                                                   final StreamsExecutionEnvironment env,
                                                   final InternalExecuted executed) {

        // Gets user-defined name or fallback on descriptor cannot be null).
        final String name = executed.nameOrElseGet(descriptor.name());

        // Gets user-defined description or fallback on descriptor (can be null).
        final String description = executed.descriptionOrElseGet(descriptor.description());

        // Gets user-defined configuration and fallback on descriptor streams config.
        final Conf streamsConfig = executed.config().withFallback(
            Conf.with("streams", descriptor.streamsConfigs())
        );

        Executed completedExecuted = Executed.as(name)
                .withConfig(streamsConfig)
                .withDescription(Optional.ofNullable(description).orElse(""));

        // Register StreamsLifeCycleInterceptor for class-loading isolation.
        completedExecuted = completedExecuted.withInterceptor(
            () -> new ClassloadingIsolationInterceptor(descriptor.getClassLoader())
        );

        // Get and register all StreamsLifeCycleInterceptors for the current streams.
        for (Supplier<StreamsLifecycleInterceptor> interceptor : getLifecycleInterceptors(Scoped.streams(name))) {
            completedExecuted = completedExecuted.withInterceptor(interceptor);
        }

        // Register StreamsLifeCycleInterceptor for AUTO_CREATE_TOPICS
        boolean autoCreateTopicsEnable = new AzkarraContextConfig(env.getConfiguration())
                .addConfiguration(getConfiguration())
                .isAutoCreateTopicsEnable();

        if (autoCreateTopicsEnable) {
            completedExecuted = completedExecuted.withInterceptor(new AutoCreateTopicsInterceptorSupplier(name));
        }

        LOG.info("Registered new topology to environment '" + env.name() + "' " +
                " for type='" + descriptor.className() + "', version='" + descriptor.version() +" '.");

        final ContextAwareTopologySupplier supplier = new ContextAwareTopologySupplier(this, descriptor);
        return env.addTopology(supplier, completedExecuted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<TopologyDescriptor> topologyProviders() {
        Collection<ComponentDescriptor<TopologyProvider>> descriptors =
            registry.findAllDescriptorsByType(TopologyProvider.class);
        Set<?> set = new HashSet<>(descriptors);
        // currently this is OK to cast to TopologyDescriptor.
        return (Set<TopologyDescriptor>)set;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StreamsExecutionEnvironment> environments() {
        initializeDefaultEnvironment();
        return List.copyOf(environments.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment getEnvironmentForNameOrCreate(final String envName) {
        Objects.requireNonNull(envName, "envName cannot be null");
        StreamsExecutionEnvironment environment = environments.get(envName);
        if (environment == null) {
            environment = DefaultStreamsExecutionEnvironment.create(envName);
            addExecutionEnvironment(environment);
        }
        return environment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment defaultExecutionEnvironment() {
        initializeDefaultEnvironment();
        return defaultEnvironment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> AzkarraContext addComponent(final Class<T> cls) {
        Objects.requireNonNull(cls, "cls cannot be null");
        reader.registerComponent(cls, registry);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> AzkarraContext addComponent(final ComponentFactory<T> factory) {
        Objects.requireNonNull(factory, "factory cannot be null");
        reader.registerComponent(factory, registry);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext addComponent(final String className) {
        Objects.requireNonNull(className, "className cannot be null");
        reader.registerComponent(className, registry);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponentForType(final Class<T> cls) {
        Objects.requireNonNull(cls, "cls cannot be null");
        return registry.getComponent(cls, contextConfig.configs());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponentForType(final Class<T> cls) {
        Objects.requireNonNull(cls, "cls cannot be null");
        return registry.getAllComponents(cls, contextConfig.configs());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopologyDescriptor getTopology(final String type) {
        Optional<ComponentDescriptor<TopologyProvider>> optional = registry.findDescriptorByAlias(type);
        if (optional.isPresent()) {
            return (TopologyDescriptor) optional.get();
        }
        throw new AzkarraException("Cannot find topology for given alias or class '" + type + "'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        if (state != State.CREATED) {
            throw new IllegalStateException(
                "The context is either already started or already stopped, cannot re-start");
        }
        initializeDefaultEnvironment();
        try {
            listeners.forEach(listeners -> listeners.onContextStart(this));
            registerShutdownHook();
            // Resolving components for streams environments with scope 'application'.
            globalInterceptors = getLifecycleInterceptors(APPLICATION_SCOPE);
            globalKafkaStreamsFactory = getKafkaStreamsFactory(APPLICATION_SCOPE).orElse(null);

            // Initialize and start all streams environments.
            for (StreamsExecutionEnvironment env : environments()) {
                initializeEnvironment(env);
                LOG.info("Starting streams environment: {}", env.name());
                env.start();
            }

            setState(State.STARTED);
        } catch (Exception e) {
            LOG.error("Unexpected error happens while starting AzkarraContext", e);
            stop(); // stop properly to close potentially open resources.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(boolean cleanUp) {
        LOG.info("Stopping Azkarra context");
        listeners.forEach(listener -> {
            try {
                listener.onContextStop(this);
            } catch (Exception e) {
                LOG.error("Unexpected error happens while invoking listener '{}#onContextStop' : ",
                    listener.getClass().getName(),
                    e);
            }
        });
        if (state == State.STARTED) {
            environments().forEach(env -> env.stop(cleanUp));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        stop(false);
        try {
            registry.close();
        } catch (final IOException e) {
            LOG.warn("Unexpected error while stopping context, " + e.getMessage());
        }
    }

    private void initializeEnvironment(final StreamsExecutionEnvironment env) {
        // Inject all streams interceptors for each environment.
        globalInterceptors.forEach(env::addStreamsLifecycleInterceptor);
        getLifecycleInterceptors(Scoped.env(env.name())).forEach(env::addStreamsLifecycleInterceptor);

        // Inject KafkaStreams streamsFactory
        Supplier<KafkaStreamsFactory> kafkaStreamsFactory =
                getKafkaStreamsFactory(Scoped.env(env.name())).orElse(globalKafkaStreamsFactory);
        if (kafkaStreamsFactory != null) {
            env.setKafkaStreamsFactory(kafkaStreamsFactory);
        }

        boolean waitForTopicsEnable = new AzkarraContextConfig(env.getConfiguration())
                .addConfiguration(getConfiguration())
                .isWaitForTopicsEnable();

        env.setWaitForTopicsToBeCreated(waitForTopicsEnable);
    }

    private Optional<Supplier<KafkaStreamsFactory>> getKafkaStreamsFactory(final Scoped scoped) {
        if (registry.isRegistered(KafkaStreamsFactory.class, scoped)) {
            return Optional.of(new ContextAwareKafkaStreamsFactorySupplier(
                    this,
                    registry.getComponent(KafkaStreamsFactory.class, scoped)
            ));
        }
        return Optional.empty();
    }

    private List<Supplier<StreamsLifecycleInterceptor>> getLifecycleInterceptors(final Scoped scoped) {
        return registry.getAllComponents(StreamsLifecycleInterceptor.class, scoped)
                .stream()
                .map(gettable -> new ContextAwareLifecycleInterceptorSupplier(this, gettable))
                .collect(Collectors.toList());
    }

    private void checkIfEnvironmentExists(final String name, final String errorMessage) {
        if (!environments.containsKey(name)) {
            throw new AzkarraContextException(errorMessage);
        }
    }

    private void initializeDefaultEnvironment() {
        if (defaultEnvironment == null) {
            defaultEnvironment = DefaultStreamsExecutionEnvironment.create(DEFAULT_ENV_NAME);
            addExecutionEnvironment(defaultEnvironment);
        }
    }

    private void registerShutdownHook() {
        if (registerShutdownHook) {
            ShutdownHook.register(this::stop);
        }
    }

    private class AutoCreateTopicsInterceptorSupplier
            extends ContextAwareComponentSupplier<StreamsLifecycleInterceptor> {

        private final Scoped scoped;

        /**
         * Creates a new {@link AutoCreateTopicsInterceptorSupplier} instance.
         * @param streams   the streams name.
         */
        AutoCreateTopicsInterceptorSupplier(final String streams) {
            super(DefaultAzkarraContext.this);
            this.scoped = Scoped.streams(streams);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StreamsLifecycleInterceptor get(final Conf configs) {
            AzkarraContextConfig contextConfig = new AzkarraContextConfig(configs);

            AutoCreateTopicsInterceptor interceptor = new AutoCreateTopicsInterceptor();
            interceptor.setNumPartitions(contextConfig.getAutoCreateTopicsNumPartition());
            interceptor.setReplicationFactor(contextConfig.getAutoCreateTopicsReplicationFactor());
            interceptor.setConfigs(contextConfig.getAutoCreateTopicsConfigs());
            interceptor.setDeleteTopicsOnStreamsClosed(contextConfig.isAutoDeleteTopicsEnable());
            interceptor.setTopics(newTopics(configs));
            return interceptor;
        }

        Collection<NewTopic> newTopics(final Conf configs) {
            return registry.getAllComponents(NewTopic.class, configs, scoped);
        }
    }
}

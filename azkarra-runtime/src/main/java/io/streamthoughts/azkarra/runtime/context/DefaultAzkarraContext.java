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
import io.streamthoughts.azkarra.api.components.ComponentClassReader;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.errors.AlreadyExistsException;
import io.streamthoughts.azkarra.api.errors.AzkarraContextException;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentRegistry;
import io.streamthoughts.azkarra.runtime.components.DefaultProviderClassReader;
import io.streamthoughts.azkarra.runtime.components.TopologyDescriptorFactory;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import io.streamthoughts.azkarra.runtime.interceptors.ClassloadingIsolationInterceptor;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import io.streamthoughts.azkarra.runtime.util.ShutdownHook;
import org.apache.kafka.streams.Topology;
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

    private Conf configuration;

    private ComponentRegistry registry;

    private final List<AzkarraContextListener> listeners;

    private ComponentClassReader reader;

    private State state;

    /**
     * Creates a new {@link DefaultAzkarraContext} instance.
     *
     * @param configuration   the context {@link Conf} instance.
     */
    private DefaultAzkarraContext(final Conf configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        this.configuration = configuration;
        this.environments = new LinkedHashMap<>();
        this.listeners = new ArrayList<>();
        setState(State.CREATED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext setComponentRegistry(final ComponentRegistry registry) {
        Objects.requireNonNull(registry, "registry cannot be null");
        this.registry = registry;
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
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext setConfiguration(final Conf configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext addConfiguration(final Conf configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        this.configuration = this.configuration.withFallback(configuration);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext addExecutionEnvironment(final StreamsExecutionEnvironment env)
            throws AlreadyExistsException {
        Objects.requireNonNull(env, "env cannot be null.");
        if (!environments.containsKey(env.name())) {
            setIfContextAwareAndGet(env);
            environments.put(env.name(), env);
            Map<String, Object> confAsMap = new TreeMap<>(env.getConfiguration().getConfAsMap());
            final String configLogs = confAsMap.entrySet()
                .stream()
                .map(e -> e.getKey() + " = " + e.getValue())
                .collect(Collectors.joining("\n\t"));
            LOG.info("Registered new streams env with name '{}' and default config :\n\t{}",
                env.name(),
                configLogs
            );
            if (state == State.STARTED) {
                env.start();
            }
            if (env.name().equals(DEFAULT_ENV_NAME)) {
                defaultEnvironment = env;
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
        return addAlreadyRegisteredTopologyToEnvironment(type, version, environment, executed);
    }

    public void setState(final State started) {
        state = started;
    }

    private ApplicationId addAlreadyRegisteredTopologyToEnvironment(final String type,
                                                                    final String version,
                                                                    final String environmentName,
                                                                    final Executed executed) {
        final StreamsExecutionEnvironment env = environments.get(environmentName);

        Optional<ComponentDescriptor<TopologyProvider>> opt = version == null ?
            registry.findLatestDescriptorByAlias(type) :
            registry.findLatestDescriptorByAliasAndVersion(type, version);

        if (opt.isPresent()) {
            final TopologyDescriptor descriptor = (TopologyDescriptor) opt.get();
            InternalExecuted partial = new InternalExecuted(executed);

            // Gets user-defined name or fallback on descriptor cannot be null).
            final String name = partial.nameOrElseGet(descriptor.name());

            // Gets user-defined description or fallback on descriptor (can be null).
            final String description = partial.descriptionOrElseGet(descriptor.description());

            // Gets user-defined configuration and fallback on descriptor streams config.
            Conf streamsConfig = partial.config().withFallback(Conf.with("streams", descriptor.streamsConfigs()));

            Executed completedExecuted = Executed.as(name).withConfig(streamsConfig);

            if (description != null) {
                completedExecuted = completedExecuted.withDescription(description);
            }

            completedExecuted = completedExecuted.withInterceptor(
                () -> new ClassloadingIsolationInterceptor(descriptor.getClassLoader())
            );

            return env.addTopology(
                new ContextTopologySupplier(type, version, env, completedExecuted),
                completedExecuted);

        } else {
            final String loggedVersion = version != null ? version : "latest";
            throw new AzkarraContextException(
               "Failed to register topology to environment '" + environmentName + "'." +
                " Cannot find any topology provider for type='" + type + "', version='" + loggedVersion +" '."
            );
        }
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
        T component = registry.getComponent(cls, configuration);
        setIfContextAwareAndGet(component);
        return component;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponentForType(final Class<T> cls) {
        Objects.requireNonNull(cls, "cls cannot be null");
        return registry.getAllComponents(cls, configuration)
            .stream()
            .map(this::setIfContextAwareAndGet)
            .collect(Collectors.toList());
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
            environments().forEach(StreamsExecutionEnvironment::start);
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

    private <T> T setIfContextAwareAndGet(final T component) {
        if (component instanceof AzkarraContextAware) {
            ((AzkarraContextAware)component).setAzkarraContext(this);
        }
        return component;
    }

    private class ContextTopologySupplier implements Supplier<TopologyProvider> {

        private final StreamsExecutionEnvironment env;
        private final String type;
        private final String version;
        private final InternalExecuted executed;

        ContextTopologySupplier(final String type,
                                final String version,
                                final StreamsExecutionEnvironment env,
                                final Executed executed) {
            this.env = env;
            this.type = type;
            this.version = version;
            this.executed = new InternalExecuted(executed);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TopologyProvider get() {
            final Conf config = executed.config().withFallback(env.getConfiguration().withFallback(configuration));

            TopologyProvider provider;
            if (version == null) {
                provider = registry.getLatestComponent(type, config);
            } else  {
                provider = registry.getVersionedComponent(type, version, config);
            }

            provider = setIfContextAwareAndGet(provider);

            if (Configurable.isConfigurable(provider.getClass())) {
                // provider have to be wrapped to not be configurable twice.
                return new ContextTopologyProvider(provider);
            }
            return provider;
        }
    }

    /**
     * A delegating {@link TopologyProvider} which is not {@link io.streamthoughts.azkarra.api.config.Configurable}.
     */
    private static class ContextTopologyProvider implements TopologyProvider {

        private final TopologyProvider delegate;

        ContextTopologyProvider(final TopologyProvider delegate) {
            Objects.requireNonNull(delegate, "delegate cannot be null");
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String version() {
            return delegate.version();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Topology get() {
            return delegate.get();
        }
    }
}

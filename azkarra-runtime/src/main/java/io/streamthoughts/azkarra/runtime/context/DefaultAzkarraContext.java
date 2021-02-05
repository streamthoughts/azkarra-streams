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
package io.streamthoughts.azkarra.runtime.context;

import io.streamthoughts.azkarra.api.ApplicationId;
import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.AzkarraContextListener;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.State;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentFactory;
import io.streamthoughts.azkarra.api.StreamsTopologyMeta;
import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorModifier;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.components.ContextAwareComponentFactory;
import io.streamthoughts.azkarra.api.components.Ordered;
import io.streamthoughts.azkarra.api.components.Restriction;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.AlreadyExistsException;
import io.streamthoughts.azkarra.api.errors.AzkarraContextException;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsEnvironmentException;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.query.InteractiveQueryService;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.streams.errors.StreamThreadExceptionHandler;
import io.streamthoughts.azkarra.runtime.StreamsExecutionEnvironmentAbstractFactory;
import io.streamthoughts.azkarra.runtime.components.ClassComponentAliasesGenerator;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentDescriptorFactory;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentFactory;
import io.streamthoughts.azkarra.runtime.components.condition.ConfigConditionalContext;
import io.streamthoughts.azkarra.runtime.env.LocalStreamsExecutionEnvironmentFactory;
import io.streamthoughts.azkarra.runtime.interceptors.AutoCreateTopicsInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.KafkaBrokerReadyInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.WaitForSourceTopicsInterceptor;
import io.streamthoughts.azkarra.runtime.modules.InteractiveQueryServiceModule;
import io.streamthoughts.azkarra.runtime.query.DefaultInteractiveQueryService;
import io.streamthoughts.azkarra.runtime.service.LocalAzkarraStreamsService;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import io.streamthoughts.azkarra.runtime.util.ShutdownHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.api.components.condition.Conditions.any;
import static io.streamthoughts.azkarra.api.components.condition.Conditions.onMissingComponent;
import static io.streamthoughts.azkarra.api.components.condition.Conditions.onPropertyTrue;
import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.withConditions;
import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.withOrder;
import static io.streamthoughts.azkarra.runtime.interceptors.AutoCreateTopicsInterceptorConfig.AUTO_CREATE_TOPICS_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.KafkaBrokerReadyInterceptorConfig.KAFKA_READY_INTERCEPTOR_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.WaitForSourceTopicsInterceptorConfig.ENABLE_WAIT_FOR_TOPICS__CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.WaitForSourceTopicsInterceptorConfig.WAIT_FOR_TOPICS_ENABLE_CONFIG;

/**
 * The AzkarraContext.
 */
public class DefaultAzkarraContext implements AzkarraContext {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAzkarraContext.class);

    public static final String DEFAULT_ENV_NAME = "default";

    /**
     * Static helper that can be used to creates a new {@link AzkarraContext} instance
     * using a default {@link ComponentRegistry} and a empty configuration.
     *
     * @return a new {@link AzkarraContext} instance.
     */
    public static DefaultAzkarraContext create() {
        return create(Conf.empty());
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraContext} instance
     * using the specified {@link ComponentRegistry} and a empty configuration.
     *
     * @return a new {@link AzkarraContext} instance.
     */
    public static DefaultAzkarraContext create(final ComponentFactory factory) {
        return new DefaultAzkarraContext(Conf.empty(), factory);
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraContext} instance
     * using a default {@link ComponentRegistry} and the specified configuration.
     *
     * @return a new {@link AzkarraContext} instance.
     */
    public static DefaultAzkarraContext create(final Conf configuration) {
        // Set all default implementations
        DefaultComponentFactory factory = new DefaultComponentFactory(new DefaultComponentDescriptorFactory());
        factory.setComponentAliasesGenerator(new ClassComponentAliasesGenerator());
        return new DefaultAzkarraContext(configuration, factory);
    }

    private boolean registerShutdownHook;

    private final Map<String, StreamsExecutionEnvironment<?>> environments;

    private final ComponentFactory componentFactory;

    private final List<AzkarraContextListener> listeners;

    private State state;

    private Conf contextConfig;

    // contains the list of topologies to register when starting.
    private final List<TopologyRegistration> topologyRegistrations = new LinkedList<>();

    /**
     * Creates a new {@link DefaultAzkarraContext} instance.
     *
     * @param configuration the context {@link Conf} instance.
     */
    private DefaultAzkarraContext(final Conf configuration, final ComponentFactory componentFactory) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        Objects.requireNonNull(componentFactory, "componentFactory cannot be null");
        this.environments = new LinkedHashMap<>();
        this.listeners = new ArrayList<>();
        this.contextConfig = configuration;
        this.componentFactory = new ContextAwareComponentFactory(this, componentFactory);

        setState(State.CREATED);
        initialize();
    }

    private void initialize() {
        // Register all built-in StreamsLifeCycleInterceptor implementations.
        registerComponent(
                AutoCreateTopicsInterceptor.class,
                withConditions(onPropertyTrue(AUTO_CREATE_TOPICS_ENABLE_CONFIG))
        );
        registerComponent(
                MonitoringStreamsInterceptor.class,
                withConditions(onPropertyTrue(MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG))
        );
        registerComponent(
                KafkaBrokerReadyInterceptor.class,
                withConditions(onPropertyTrue(KAFKA_READY_INTERCEPTOR_ENABLE_CONFIG)),
                withOrder(Ordered.HIGHEST_ORDER)
        );
        registerComponent(
                WaitForSourceTopicsInterceptor.class,
                withConditions(any(
                        onPropertyTrue(WAIT_FOR_TOPICS_ENABLE_CONFIG),
                        onPropertyTrue(ENABLE_WAIT_FOR_TOPICS__CONFIG))
                ),
                withOrder(Ordered.LOWEST_ORDER)
        );
        registerComponent(
                StreamThreadExceptionHandler.class,
                new DefaultStreamThreadExceptionHandlerFactory(),
                withConditions(onMissingComponent(List.of(StreamThreadExceptionHandler.class)))
        );
        registerSingleton(
                AzkarraStreamsService.class,
                LocalAzkarraStreamsService::new,
                withConditions(onMissingComponent(List.of(AzkarraStreamsService.class)))
        );
        registerSingleton(
                StreamsExecutionEnvironmentFactory.class,
                LocalStreamsExecutionEnvironmentFactory::new,
                withConditions(onMissingComponent(List.of(StreamsExecutionEnvironmentFactory.class)))
        );
        registerComponent(
                StreamsExecutionEnvironmentAbstractFactory.class,
                () -> new StreamsExecutionEnvironmentAbstractFactory(getAllComponents(StreamsExecutionEnvironmentFactory.class))
        );

        registerSingleton(
                DefaultInteractiveQueryService.class,
                new InteractiveQueryServiceModule(),
                withConditions(onMissingComponent(List.of(InteractiveQueryService.class)))
        );

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentFactory getComponentFactory() {
        return componentFactory;
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
        return contextConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext setConfiguration(final Conf configuration) {
        contextConfig = configuration;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext addConfiguration(final Conf configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        contextConfig = contextConfig.withFallback(configuration);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AzkarraContext addExecutionEnvironment(final StreamsExecutionEnvironment<?> env)
            throws AlreadyExistsException {
        Objects.requireNonNull(env, "env cannot be null.");
        LOG.debug("Adding new streams environment for name '{}'", env.name());
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
                ((AzkarraContextAware) env).setAzkarraContext(this);

            if (state == State.STARTED) {
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
    public Optional<ApplicationId> addTopology(final String type, final Executed executed) {
        return addTopology(type, null, executed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ApplicationId> addTopology(final Class<? extends TopologyProvider> type, final Executed executed) {
        return addTopology(type, null, executed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ApplicationId> addTopology(final Class<? extends TopologyProvider> type,
                                               final String environment,
                                               final Executed executed) {
        return addTopology(type.getName(), environment, executed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ApplicationId> addTopology(final String type,
                                               final String environment,
                                               final Executed executed) {
        return addTopology(type, null, environment, executed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ApplicationId> addTopology(final String type,
                                               final String version,
                                               final String environment,
                                               final Executed executed) {
        final TopologyRegistration registration = new TopologyRegistration(type, version, environment, executed);
        if (state == State.STARTED)
            return addTopologyToEnvironment(registration);
        else {
            topologyRegistrations.add(registration);
            return Optional.empty();
        }

    }

    public void setState(final State state) {
        this.state = state;
    }

    /**
     * Build and add a {@link TopologyProvider} to a target {@link StreamsExecutionEnvironment}.
     *
     * @param registration the {@link TopologyRegistration}
     *
     * @return the {@link ApplicationId} instance if the environment is already started, otherwise {@code null}.
     * @throws AzkarraContextException if no component is registered for the topology to register.
     *                                 if target environment doesn't exists.
     */
    private Optional<ApplicationId> addTopologyToEnvironment(final TopologyRegistration registration) {
        Objects.requireNonNull(registration, "registration cannot be null");

        final StreamsExecutionEnvironment<?> environment;
        if (registration.environment != null) {
            checkIfEnvironmentExists(
                    registration.environment,
                    String.format(
                            "Error while adding topology '%s', environment '%s' not found",
                            registration.type,
                            registration.environment
                    ));
            environment = environments.get(registration.environment);
        } else {
            environment = getDefaultEnvironment();
        }

        // Attempt to register the topology as a component if it is missing.
        if (!componentFactory.containsComponent(registration.type)) {
            try {
                registerComponent(Class.forName(registration.type));
            } catch (final AzkarraException | ClassNotFoundException e) {
                /* can ignore this exception for now, initialization will fail later */
            }
        }

        final var streamConfig = registration.executed.config();
        final var conditionalContext = new ConfigConditionalContext<>(
                streamConfig
                        .withFallback(environment.getConfiguration())
                        .withFallback(getConfiguration())
        );

        final Optional<ComponentDescriptor<TopologyProvider>> opt = registration.version == null ?
                componentFactory.findDescriptorByAlias(registration.type, conditionalContext, Qualifiers.byLatestVersion()) :
                componentFactory.findDescriptorByAlias(registration.type, conditionalContext, Qualifiers.byVersion(registration.version));

        if (opt.isPresent()) {
            final TopologyDescriptor<TopologyProvider> descriptor = new TopologyDescriptor<>(opt.get());
            final StreamsTopologyMeta meta = StreamsTopologyMeta.create().from(descriptor).build();
            return environment.newTopologyExecution(meta, registration.executed).start();

        } else {
            final String loggedVersion = registration.version != null ? registration.version : "latest";
            throw new AzkarraContextException(
                    "Failed to register topology to environment '" + environment.name() + "'."
                            + " Cannot find any topology provider for type='" + registration.type
                            + "', version='" + loggedVersion + " '."
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TopologyDescriptor> getTopologyDescriptors() {
        Collection<ComponentDescriptor<TopologyProvider>> descriptors =
                componentFactory.findAllDescriptorsByClass(TopologyProvider.class);
        return descriptors.stream().map(TopologyDescriptor::new).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TopologyDescriptor> getTopologyDescriptors(final String environmentName) {
        return getTopologyDescriptors(getEnvironmentForName(environmentName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TopologyDescriptor> getTopologyDescriptors(final StreamsExecutionEnvironment<?> env) {
        final Conf componentConfig = env.getConfiguration()
                .withFallback(getConfiguration());
        var conditionalContext = new ConfigConditionalContext<>(componentConfig);
        Collection<ComponentDescriptor<TopologyProvider>> descriptors =
                componentFactory.findAllDescriptorsByClass(
                        TopologyProvider.class,
                        conditionalContext, Qualifiers.byAnyRestrictions(
                                Restriction.application(),
                                Restriction.env(env.name())
                        )
                );
        return descriptors.stream().map(TopologyDescriptor::new).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StreamsExecutionEnvironment<?>> getAllEnvironments() {
        return List.copyOf(environments.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment<?> getEnvironmentForName(final String name) {
        Objects.requireNonNull(name, "name cannot be null");
        checkIfEnvironmentExists(name, "Failed to find environment for name '" + name + "'");
        return environments.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsExecutionEnvironment<?> getDefaultEnvironment() {
        return getAllEnvironments()
                .stream()
                .filter(env -> env.isDefault() || env.name().equals(DEFAULT_ENV_NAME))
                .findFirst()
                .orElse(null);

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
        LOG.info("Starting AzkarraContext");
        componentFactory.init(getConfiguration());
        registerShutdownHook();
        try {
            listeners.addAll(getAllComponents(AzkarraContextListener.class));
            Collections.sort(listeners);
            listeners.forEach(listener -> {
                LOG.debug("Executing context listener {}#onContextStart", listener.getClass());
                listener.onContextStart(this);
            });
            preStart();
            // Initialize and start all streams environments.
            for (StreamsExecutionEnvironment<?> env : getAllEnvironments()) {
                LOG.info("Starting streams execution environment: {}", env.name());
                env.start();
            }
            setState(State.STARTED);
        } catch (Exception e) {
            LOG.error("Unexpected error happens while starting AzkarraContext", e);
            stop(); // stop properly to close potentially open resources.
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(boolean cleanUp) {
        LOG.info("Stopping AzkarraContext");
        Collections.sort(listeners);
        listeners.forEach(listener -> {
            try {
                LOG.debug("Executing context listener {}#onContextStop", listener.getClass());
                listener.onContextStop(this);
            } catch (Exception e) {
                LOG.error(
                        "Unexpected error happens while invoking listener '{}#onContextStop' : ",
                        listener.getClass().getName(),
                        e
                );
            }
        });
        if (state == State.STARTED) {
            getAllEnvironments().forEach(env -> env.stop(cleanUp));
        }
        try {
            componentFactory.close();
        } catch (final IOException e) {
            LOG.warn("Unexpected error while stopping context, " + e.getMessage());
        }
        LOG.info("AzkarraContext stopped completely");
    }

    @VisibleForTesting
    void preStart() {
        initDefaultStreamsExecutionEnvironment();
        if (!topologyRegistrations.isEmpty()) {
            LOG.info("Adding all registered topologies to declared streams environments");
            topologyRegistrations.forEach(this::addTopologyToEnvironment);
        }
    }

    private void checkIfEnvironmentExists(final String name, final String errorMessage) {
        if (!environments.containsKey(name)) {
            throw new InvalidStreamsEnvironmentException(errorMessage);
        }
    }

    private void initDefaultStreamsExecutionEnvironment() {
        final List<StreamsExecutionEnvironment<?>> defaults = environments
                .values()
                .stream()
                .filter(env -> env.isDefault() || env.name().equals(DEFAULT_ENV_NAME))
                .collect(Collectors.toList());

        if (defaults.size() > 1) {
            final String strDefault = defaults
                    .stream()
                    .map(StreamsExecutionEnvironment::name)
                    .collect(Collectors.joining(",", "[", "]"));
            throw new AzkarraContextException("Too many default environments " + strDefault);
        }

        if (defaults.size() == 1) {
            // Ensure the environment named 'default' is flagged as default.
            defaults.get(0).isDefault(true);
        }

        if (defaults.isEmpty()) {
            LOG.warn("No default environment can be found, initializing a new one with name {}", DEFAULT_ENV_NAME);
            addExecutionEnvironment(
                    getComponent(StreamsExecutionEnvironmentFactory.class)
                            .create(DEFAULT_ENV_NAME)
                            .isDefault(true)
            );
        }
    }

    private void registerShutdownHook() {
        if (registerShutdownHook) {
            ShutdownHook.register(this::stop);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final String componentName,
                                      final Class<T> componentClass,
                                      final Supplier<T> supplier,
                                      final ComponentDescriptorModifier... modifiers) {
        componentFactory.registerComponent(componentName, componentClass, supplier, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final String componentName,
                                      final Class<T> componentClass,
                                      final ComponentDescriptorModifier... modifiers) {
        componentFactory.registerComponent(componentName, componentClass, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerSingleton(final String componentName,
                                      final Class<T> componentClass,
                                      final Supplier<T> singleton,
                                      final ComponentDescriptorModifier... modifiers) {
        componentFactory.registerSingleton(componentName, componentClass, singleton, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerSingleton(final String componentName,
                                      final Class<T> componentClass,
                                      final ComponentDescriptorModifier... modifiers) {
        componentFactory.registerSingleton(componentName, componentClass, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerSingleton(final T singleton) {
        componentFactory.registerSingleton(singleton);
    }

    /**
     * This class represents a topology identified by a type/version/executed
     * to register to a target streams environment.
     */
    private static class TopologyRegistration {

        final String type;
        final String version;
        final String environment;
        final InternalExecuted executed;

        TopologyRegistration(final String type,
                             final String version,
                             final String environment,
                             final Executed executed) {
            this.type = Objects.requireNonNull(type, "type should not be null");
            this.environment = environment;
            this.version = version;
            this.executed = new InternalExecuted(executed);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TopologyRegistration)) return false;
            TopologyRegistration that = (TopologyRegistration) o;
            return Objects.equals(type, that.type) &&
                    Objects.equals(version, that.version) &&
                    Objects.equals(environment, that.environment) &&
                    Objects.equals(executed, that.executed);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(type, version, environment, executed);
        }
    }
}

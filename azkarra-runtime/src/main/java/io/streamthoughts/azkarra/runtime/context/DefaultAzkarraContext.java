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
import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorModifier;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.components.ContextAwareComponentFactory;
import io.streamthoughts.azkarra.api.components.GettableComponent;
import io.streamthoughts.azkarra.api.components.Ordered;
import io.streamthoughts.azkarra.api.components.Qualifier;
import io.streamthoughts.azkarra.api.components.Restriction;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.AlreadyExistsException;
import io.streamthoughts.azkarra.api.errors.AzkarraContextException;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsEnvironmentException;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsFactory;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.streams.errors.StreamThreadExceptionHandler;
import io.streamthoughts.azkarra.runtime.components.ClassComponentAliasesGenerator;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentDescriptorFactory;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentFactory;
import io.streamthoughts.azkarra.runtime.components.condition.ConfigConditionalContext;
import io.streamthoughts.azkarra.runtime.context.internal.ClassLoaderAwareKafkaStreamsFactory;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareApplicationIdBuilderSupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareKafkaStreamsFactorySupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareLifecycleInterceptorSupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareThreadExceptionHandlerSupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareTopologySupplier;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import io.streamthoughts.azkarra.runtime.interceptors.AutoCreateTopicsInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.ClassloadingIsolationInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.KafkaBrokerReadyInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.WaitForSourceTopicsInterceptor;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import io.streamthoughts.azkarra.runtime.util.ShutdownHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

import static io.streamthoughts.azkarra.api.components.condition.Conditions.onMissingComponent;
import static io.streamthoughts.azkarra.api.components.condition.Conditions.onPropertyTrue;
import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.withConditions;
import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.withOrder;
import static io.streamthoughts.azkarra.runtime.interceptors.AutoCreateTopicsInterceptorConfig.AUTO_CREATE_TOPICS_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.KafkaBrokerReadyInterceptorConfig.KAFKA_READY_INTERCEPTOR_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.WaitForSourceTopicsInterceptorConfig.WAIT_FOR_TOPICS_ENABLE_CONFIG;

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
    public static AzkarraContext create(final ComponentFactory factory) {
        return new DefaultAzkarraContext(Conf.empty(), factory);
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraContext} instance
     * using a default {@link ComponentRegistry} and the specified configuration.
     *
     * @return a new {@link AzkarraContext} instance.
     */
    public static AzkarraContext create(final Conf configuration) {
        // Set all default implementations
        DefaultComponentFactory factory = new DefaultComponentFactory(new DefaultComponentDescriptorFactory());
        factory.setComponentAliasesGenerator(new ClassComponentAliasesGenerator());
        return new DefaultAzkarraContext(configuration, factory);
    }

    private boolean registerShutdownHook;

    private StreamsExecutionEnvironment defaultEnvironment;

    private Map<String, StreamsExecutionEnvironment> environments;

    private ComponentFactory componentFactory;

    private final List<AzkarraContextListener> listeners;

    private State state;

    private RestrictedComponentSupplierFactory componentSupplierFactory;

    private Conf contextConfig;

    // contains the list of topologies to register when starting.
    private final List<TopologyRegistration> topologyRegistrations = new LinkedList<>();

    /**
     * Creates a new {@link DefaultAzkarraContext} instance.
     *
     * @param configuration   the context {@link Conf} instance.
     */
    private DefaultAzkarraContext(final Conf configuration, final ComponentFactory componentFactory) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        Objects.requireNonNull(componentFactory, "componentFactory cannot be null");
        this.environments = new LinkedHashMap<>();
        this.listeners = new ArrayList<>();
        this.contextConfig = configuration;
        this.componentFactory = new ContextAwareComponentFactory(this, componentFactory);
        this.componentSupplierFactory = new RestrictedComponentSupplierFactory(this);
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
            withConditions(onPropertyTrue(WAIT_FOR_TOPICS_ENABLE_CONFIG)),
            withOrder(Ordered.LOWEST_ORDER)
        );

        registerComponent(
            StreamThreadExceptionHandler.class,
            new DefaultStreamThreadExceptionHandlerFactory(),
            withConditions(onMissingComponent(List.of(StreamThreadExceptionHandler.class)))
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
    public AzkarraContext addExecutionEnvironment(final StreamsExecutionEnvironment env)
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

        final TopologyRegistration registration = new TopologyRegistration(type, version, environment, executed);

        ApplicationId res = null;
        if (state == State.STARTED)
           res = mayAddTopologyToEnvironment(registration);
        else
            topologyRegistrations.add(registration);
        return res;
    }

    public void setState(final State state) {
        this.state = state;
    }

    /**
     * Build and add a {@link TopologyProvider} to a target {@link StreamsExecutionEnvironment}.
     *
     * @param registration  the {@link TopologyRegistration}
     * @return              the {@link ApplicationId} instance if the environment is already started,
     *                      otherwise {@code null}.
     *
     * @throws AzkarraContextException if no component is registered for the topology to register.
     *                                 if target environment doesn't exists.
     */
    private ApplicationId mayAddTopologyToEnvironment(final TopologyRegistration registration) {
        Objects.requireNonNull(registration, "registration cannot be null");
        checkIfEnvironmentExists(
            registration.environment,
            String.format(
                "Error while adding topology '%s', environment '%s' not found",
                registration.type,
                registration.environment
        ));

        final var environment = environments.get(registration.environment);

        // Attempt to register the topology as a component if it is missing.
        if (!componentFactory.containsComponent(registration.type)) {
            try {
                registerComponent(Class.forName(registration.type));
            } catch (final AzkarraException | ClassNotFoundException e) {
                /* can ignore this exception for now, initialization will fail later */
            }
        }

        final var streamConfig = registration.executed.config();
        final var conditionalContext = new ConfigConditionalContext(
            streamConfig
                .withFallback(environment.getConfiguration())
                .withFallback(getConfiguration())
        );

        final Optional<ComponentDescriptor<TopologyProvider>> opt = registration.version == null ?
            componentFactory.findDescriptorByAlias(registration.type, conditionalContext, Qualifiers.byLatestVersion()) :
            componentFactory.findDescriptorByAlias(registration.type, conditionalContext, Qualifiers.byVersion(registration.version));

        if (opt.isPresent()) {
            final TopologyDescriptor descriptor = new TopologyDescriptor<>(opt.get());
            return addTopologyToEnvironment(descriptor, environment, registration.executed);

        } else {
            final String loggedVersion = registration.version != null ? registration.version : "latest";
            throw new AzkarraContextException(
               "Failed to register topology to environment '" + registration.environment + "'."
               + " Cannot find any topology provider for type='" + registration.type
               + "', version='" + loggedVersion +" '."
            );
        }
    }

    @VisibleForTesting
    ApplicationId addTopologyToEnvironment(final TopologyDescriptor<?> descriptor,
                                           final StreamsExecutionEnvironment env,
                                           final InternalExecuted executed) {

        // Gets user-defined streams name or fallback on descriptor cannot be null).
        final var streamName = executed.nameOrElseGet(descriptor.name());

        // Gets user-defined description or fallback on descriptor (can be null).
        final var description = executed.descriptionOrElseGet(descriptor.description());

        // Gets user-defined configuration and fallback on inherited configurations.
        final var streamsConfig = Conf.of(
            executed.config(),          // (1) Executed
            env.getConfiguration(),     // (2) Environment
            getConfiguration(),         // (3) Context
            descriptor.configuration()  // (4) Descriptor (i.e: default)
        );

        final var interceptors = executed.interceptors();
        // Register StreamsLifeCycleInterceptor for class-loading isolation (must always be registered first)
        interceptors.add(() -> new ClassloadingIsolationInterceptor(descriptor.classLoader()));

        // Get and register all StreamsLifeCycleInterceptors component for any scopes: Application, Env, Streams
        interceptors.addAll(componentSupplierFactory.findLifecycleInterceptors(
            streamsConfig,
            Restriction.application(),
            Restriction.env(env.name()),
            Restriction.streams(streamName))
        );

        // Get and register KafkaStreamsFactory for one the scopes: Application, Env, Streams
        final var factory =  executed.factory()
            .or(() -> componentSupplierFactory.findKafkaStreamsFactory(streamsConfig, Restriction.streams(streamName)))
            .or(() -> componentSupplierFactory.findKafkaStreamsFactory(streamsConfig, Restriction.env(env.name())))
            .or(() -> componentSupplierFactory.findKafkaStreamsFactory(streamsConfig, Restriction.application()))
            .orElse(() -> KafkaStreamsFactory.DEFAULT);

        LOG.info(
            "Registered new topology to environment '{}' for type='{}', version='{}', name='{}'.",
            env.name(),
            descriptor.className() ,
            descriptor.version(),
            streamName
        );

        final var completedExecuted = Executed.as(streamName)
            .withConfig(streamsConfig)
            .withDescription(Optional.ofNullable(description).orElse(""))
            .withInterceptors(interceptors)
            .withKafkaStreamsFactory(
                () -> new ClassLoaderAwareKafkaStreamsFactory(factory.get(), descriptor.classLoader())
            );

        return env.addTopology(new ContextAwareTopologySupplier(this, descriptor), completedExecuted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TopologyDescriptor> topologyProviders() {
        Collection<ComponentDescriptor<TopologyProvider>> descriptors =
            componentFactory.findAllDescriptorsByClass(TopologyProvider.class);
        return descriptors.stream().map(TopologyDescriptor::new).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TopologyDescriptor> topologyProviders(final StreamsExecutionEnvironment env) {
        final Conf componentConfig = env.getConfiguration()
                .withFallback(getConfiguration());
        ConfigConditionalContext conditionalContext = new ConfigConditionalContext(componentConfig);
        Collection<ComponentDescriptor<TopologyProvider>> descriptors =
                componentFactory.findAllDescriptorsByClass(TopologyProvider.class, conditionalContext);
        return descriptors.stream().map(TopologyDescriptor::new).collect(Collectors.toSet());
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
    public TopologyDescriptor getTopology(final String type) {
        Optional<ComponentDescriptor<TopologyProvider>> descriptor = componentFactory.findDescriptorByAlias(type);
        if (descriptor.isPresent()) {
            return new TopologyDescriptor<>(descriptor.get());
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
        LOG.info("Starting AzkarraContext");
        preStart();
        componentFactory.init(getConfiguration());
        try {
            listeners.forEach(listeners -> listeners.onContextStart(this));
            registerShutdownHook();
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

    @VisibleForTesting
    void preStart() {
        initializeDefaultEnvironment();
        if (!topologyRegistrations.isEmpty()) {
            LOG.info("Adding all registered topologies to declared streams environments");
            topologyRegistrations.forEach(this::mayAddTopologyToEnvironment);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(boolean cleanUp) {
        LOG.info("Stopping AzkarraContext");
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
        try {
            componentFactory.close();
        } catch (final IOException e) {
            LOG.warn("Unexpected error while stopping context, " + e.getMessage());
        }
        LOG.info("AzkarraContext stopped completely");
    }

    private void initializeEnvironment(final StreamsExecutionEnvironment env) {

        final Conf componentResolutionConfig = env.getConfiguration().withFallback(getConfiguration());

        // Inject environment or application scoped ApplicationIdBuilder
        Optional.ofNullable(env.getApplicationIdBuilder())
            .or(() -> componentSupplierFactory.findApplicationIdBuilder(componentResolutionConfig,  Restriction.env(env.name())))
            .or(() -> componentSupplierFactory.findApplicationIdBuilder(componentResolutionConfig, Restriction.application()))
            .ifPresent(env::setApplicationIdBuilder);

        // Inject environment, global or default StreamsThreadExceptionHandler
        Optional.ofNullable(env.getStreamThreadExceptionHandler())
            .or(() -> componentSupplierFactory.findStreamThreadExceptionHandler(componentResolutionConfig, Restriction.env(env.name())))
            .or(() -> componentSupplierFactory.findStreamThreadExceptionHandler(componentResolutionConfig, Restriction.application()))
            .ifPresent(env::setStreamThreadExceptionHandler);
    }

    private void checkIfEnvironmentExists(final String name, final String errorMessage) {
        if (!environments.containsKey(name)) {
            throw new InvalidStreamsEnvironmentException(errorMessage);
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
            this.type = type;
            this.version = version;
            this.environment = environment;
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

    @VisibleForTesting
    static class RestrictedComponentSupplierFactory {

        private final ComponentFactory factory;
        private final AzkarraContext context;

        RestrictedComponentSupplierFactory(final AzkarraContext context) {
            this.context = context;
            this.factory = context.getComponentFactory();
        }

        List<Supplier<StreamsLifecycleInterceptor>> findLifecycleInterceptors(final Conf componentConfig,
                                                                              final Restriction... restrictions) {
            final Qualifier<StreamsLifecycleInterceptor> qualifier = Qualifiers.byAnyRestrictions(restrictions);
            return factory.getAllComponentProviders(StreamsLifecycleInterceptor.class, qualifier)
                .stream()
                .filter(ConfigConditionalContext.of(componentConfig))
                .map(provider -> new ContextAwareLifecycleInterceptorSupplier(context, provider))
                .collect(Collectors.toList());
        }

        Optional<Supplier<ApplicationIdBuilder>> findApplicationIdBuilder(final Conf componentConfig,
                                                                          final Restriction restriction) {
            return findComponentByRestriction(ApplicationIdBuilder.class, componentConfig, restriction)
                    .map(gettable -> new ContextAwareApplicationIdBuilderSupplier(context, gettable));
        }

        Optional<Supplier<KafkaStreamsFactory>> findKafkaStreamsFactory(final Conf componentConfig,
                                                                        final Restriction restriction) {
            return findComponentByRestriction(KafkaStreamsFactory.class, componentConfig, restriction)
                    .map(gettable -> new ContextAwareKafkaStreamsFactorySupplier(context, gettable));
        }

        Optional<Supplier<StreamThreadExceptionHandler>> findStreamThreadExceptionHandler(final Conf conf,
                                                                                          final Restriction restriction) {
            return findComponentByRestriction(StreamThreadExceptionHandler.class, conf, restriction)
                    .map(gettable -> new ContextAwareThreadExceptionHandlerSupplier(context, gettable));
        }

        /**
         * Finds a component for the given type that is available for the given config and restriction.
         *
         * @param componentType        the {@link Class } of the component.
         * @param componentConfig      the {@link Conf} object to be used for resolved available components.
         * @param restriction          the {@link Restriction}.
         * @param <T>                  the component type.
         * @return                     an optional {@link GettableComponent}.
         */
        private <T> Optional<GettableComponent<T>> findComponentByRestriction(final Class<T> componentType,
                                                                              final Conf componentConfig,
                                                                              final Restriction restriction) {

            final Qualifier<T> qualifier = Qualifiers.byRestriction(restriction);
            if (factory.containsComponent(componentType, qualifier)) {
                GettableComponent<T> provider = factory.getComponentProvider(componentType, qualifier);
                if (provider.isEnable(new ConfigConditionalContext<>(componentConfig)))
                    return Optional.of(provider);
            }
            return Optional.empty();
        }
    }
}

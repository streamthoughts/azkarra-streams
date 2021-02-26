/*
 * Copyright 2019-2021 StreamThoughts.
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

import io.streamthoughts.azkarra.api.ApplicationId;
import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.StreamsTopologyMeta;
import io.streamthoughts.azkarra.api.components.Qualifier;
import io.streamthoughts.azkarra.api.components.Restriction;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.config.ConfigurableSupplier;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsFactory;
import io.streamthoughts.azkarra.runtime.AbstractTopologyStreamsExecution;
import io.streamthoughts.azkarra.runtime.components.RestrictedComponentFactory;
import io.streamthoughts.azkarra.runtime.components.condition.ConfigConditionalContext;
import io.streamthoughts.azkarra.runtime.context.internal.ClassLoaderAwareKafkaStreamsFactory;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareKafkaStreamsFactorySupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareLifecycleInterceptorSupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareTopologySupplier;
import io.streamthoughts.azkarra.runtime.interceptors.ClassloadingIsolationInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LocalStreamsExecution extends AbstractTopologyStreamsExecution<LocalStreamsExecutionEnvironment> {

    private static final Logger LOG = LoggerFactory.getLogger(LocalStreamsExecution.class);
    private final AzkarraContext context;

    /**
     * Creates a new {@link LocalStreamsExecution} instance.
     *
     * @param meta        the {@link StreamsTopologyMeta} object.
     * @param executed    the {@link Executed} object.
     * @param context     the {@link AzkarraContext} object.
     * @param environment the {@link LocalStreamsExecutionEnvironment} object.
     */
    LocalStreamsExecution(final StreamsTopologyMeta meta,
                          final Executed executed,
                          final AzkarraContext context,
                          final LocalStreamsExecutionEnvironment environment) {
        super(environment, meta, executed);
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ApplicationId> start() {
        // Gets user-defined streams name or fallback on descriptor cannot be null).
        final var streamName = executed.nameOrElseGet(meta.name());

        // Gets user-defined description or fallback on descriptor (can be null).
        final var description = executed.descriptionOrElseGet(meta.description());

        // Gets user-defined configuration and fallback on inherited configurations.
        final var streamsConfig = Conf.of(
                executed.config(),              // (1) Executed
                environment.getConfiguration(), // (2) Environment
                context.getConfiguration(),     // (3) Context
                meta.configuration()            // (4) Provider (i.e: default)
        );

        final var interceptors = executed.interceptors();
        // Register StreamsLifeCycleInterceptor for class-loading isolation (must always be registered first)
        interceptors.add(() -> new ClassloadingIsolationInterceptor(meta.classLoader()));

        // Get and register all StreamsLifeCycleInterceptors component for any scopes: Application, Env, Streams
        interceptors.addAll(findLifecycleInterceptors(
                streamsConfig,
                Restriction.application(),
                Restriction.env(environment.name()),
                Restriction.streams(streamName))
        );

        LOG.info(
                "Registered new topology to environment '{}' for type='{}', version='{}', name='{}'.",
                environment.name(),
                meta.type().getName(),
                meta.version(),
                streamName
        );

        // Get and register KafkaStreamsFactory for one the scopes: Application, Env, Streams
        final var factory = findStreamsFactorySupplierFor(streamName);

        final var completedExecuted = Executed.as(streamName)
                .withConfig(streamsConfig)
                .withDescription(Optional.ofNullable(description).orElse(""))
                .withInterceptors(interceptors)
                .withKafkaStreamsFactory(factory);

        return environment.addTopology(new ContextAwareTopologySupplier(context, meta), completedExecuted);
    }

    private ConfigurableSupplier<KafkaStreamsFactory> findStreamsFactorySupplierFor(final String streamName) {
        return new ConfigurableSupplier<>() {
            @Override
            public KafkaStreamsFactory get(final Conf configs) {
                Supplier<KafkaStreamsFactory> factory = executed.factory()
                    .or(() -> findKafkaStreamsFactory(configs, Restriction.streams(streamName)))
                    .or(() -> findKafkaStreamsFactory(configs, Restriction.env(environment.name())))
                    .or(() -> findKafkaStreamsFactory(configs, Restriction.application()))
                    .orElse(() -> KafkaStreamsFactory.DEFAULT);

                Configurable.mayConfigure(factory, configs);

                return new ClassLoaderAwareKafkaStreamsFactory(factory.get(), meta.classLoader());
            }
        };
    }

    private List<Supplier<StreamsLifecycleInterceptor>> findLifecycleInterceptors(final Conf componentConfig,
                                                                                  final Restriction... restrictions) {
        final Qualifier<StreamsLifecycleInterceptor> qualifier = Qualifiers.byAnyRestrictions(restrictions);
        return context.getComponentFactory()
                .getAllComponentProviders(StreamsLifecycleInterceptor.class, qualifier)
                .stream()
                .filter(ConfigConditionalContext.of(componentConfig))
                .map(provider -> new ContextAwareLifecycleInterceptorSupplier(context, provider))
                .collect(Collectors.toList());
    }

    private Optional<Supplier<KafkaStreamsFactory>> findKafkaStreamsFactory(final Conf componentConfig,
                                                                            final Restriction restriction) {
        return new RestrictedComponentFactory(context.getComponentFactory())
                .findComponentByRestriction(KafkaStreamsFactory.class, componentConfig, restriction)
                .map(gettable -> new ContextAwareKafkaStreamsFactorySupplier(context, gettable));
    }
}

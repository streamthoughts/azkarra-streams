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
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentFactory;
import io.streamthoughts.azkarra.api.components.Restriction;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.errors.StreamThreadExceptionHandler;
import io.streamthoughts.azkarra.runtime.components.RestrictedComponentFactory;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareApplicationIdBuilderSupplier;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareThreadExceptionHandlerSupplier;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The default {@link StreamsExecutionEnvironment} implementation.
 */
public class LocalStreamsExecutionEnvironmentFactory
    implements StreamsExecutionEnvironmentFactory<LocalStreamsExecutionEnvironment>, AzkarraContextAware {

    private AzkarraContext context;

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalStreamsExecutionEnvironment create(final String name, final Conf conf) {
        return initialize(LocalStreamsExecutionEnvironment.create(name, conf));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return LocalStreamsExecutionEnvironment.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAzkarraContext(final AzkarraContext context) {
        this.context = context;
    }

    private LocalStreamsExecutionEnvironment initialize(final LocalStreamsExecutionEnvironment env) {

        final Conf componentResolutionConfig = env.getConfiguration().withFallback(context.getConfiguration());

        // Inject environment or application scoped ApplicationIdBuilder
        Optional.ofNullable(env.getApplicationIdBuilder())
            .or(() -> findApplicationIdBuilder(componentResolutionConfig, Restriction.env(env.name())))
            .or(() -> findApplicationIdBuilder(componentResolutionConfig, Restriction.application()))
            .ifPresent(env::setApplicationIdBuilder);

        // Inject environment, global or default StreamsThreadExceptionHandler
        Optional.ofNullable(env.getStreamThreadExceptionHandler())
            .or(() -> findStreamThreadExceptionHandler(componentResolutionConfig, Restriction.env(env.name())))
            .or(() -> findStreamThreadExceptionHandler(componentResolutionConfig, Restriction.application()))
            .ifPresent(env::setStreamThreadExceptionHandler);

        return env;
    }

    public Optional<Supplier<ApplicationIdBuilder>> findApplicationIdBuilder(final Conf componentConfig,
                                                                             final Restriction restriction) {
        return new RestrictedComponentFactory(context.getComponentFactory())
                .findComponentByRestriction(ApplicationIdBuilder.class, componentConfig, restriction)
                .map(gettable -> new ContextAwareApplicationIdBuilderSupplier(context, gettable));
    }

    public Optional<Supplier<StreamThreadExceptionHandler>> findStreamThreadExceptionHandler(
            final Conf conf,
            final Restriction restriction) {
        return new RestrictedComponentFactory(context.getComponentFactory())
                .findComponentByRestriction(StreamThreadExceptionHandler.class, conf, restriction)
                .map(gettable -> new ContextAwareThreadExceptionHandlerSupplier(context, gettable));
    }
}
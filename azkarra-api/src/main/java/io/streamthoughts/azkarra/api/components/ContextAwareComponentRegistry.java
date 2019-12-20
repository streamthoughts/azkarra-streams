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
package io.streamthoughts.azkarra.api.components;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.config.Conf;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class ContextAwareComponentRegistry implements ComponentRegistry {

    private final AzkarraContext context;

    private final ComponentRegistry registry;

    /**
     * Creates a new {@link ContextAwareComponentRegistry} instance.
     *
     * @param context   the {@link AzkarraContext}.
     */
    public ContextAwareComponentRegistry(final AzkarraContext context,
                                         final ComponentRegistry registry) {
        this.context = context;
        this.registry = registry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegistered(final String alias) {
        return registry.isRegistered(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegistered(final Class<?> type) {
        return registry.isRegistered(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegistered(final Class<?> type, final Scoped scoped) {
        return registry.isRegistered(type, scoped);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias) {
        return registry.findDescriptorByAlias(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findLatestDescriptorByAlias(final String alias) {
        return registry.findLatestDescriptorByAlias(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findLatestDescriptorByAliasAndVersion(final String alias,
                                                                                      final String version) {
        return registry.findLatestDescriptorByAliasAndVersion(alias, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorByAlias(final String alias) {
        return registry.findAllDescriptorByAlias(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByType(final Class<T> type) {
        return registry.findAllDescriptorsByType(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf, final Scoped scoped) {
        return maySetContextAndGet(registry.getComponent(type, conf, scoped));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> GettableComponent<T> getComponent(final Class<T> type, final Scoped scoped) {
        return registry.getComponent(type, scoped);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getLatestComponent(final Class<T> type, final Conf conf, final Scoped scoped) {
        return maySetContextAndGet(registry.getLatestComponent(type, conf, scoped));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final String alias, final Conf conf, final Scoped scoped) {
        return maySetContextAndGet(registry.getComponent(alias, conf, scoped));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getLatestComponent(final String alias, final Conf conf, final Scoped scoped) {
        return maySetContextAndGet(registry.getLatestComponent(alias, conf, scoped));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getVersionedComponent(final String alias,
                                       final String version,
                                       final Conf conf,
                                       final Scoped scoped) {
        return maySetContextAndGet(registry.getVersionedComponent(alias, version, conf, scoped));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final String alias, final Conf conf, final Scoped scoped) {
        Collection<T> components = registry.getAllComponents(alias, conf, scoped);
        return components.stream()
            .map(this::maySetContextAndGet)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf, final Scoped scoped) {
        return registry.getAllComponents(type, conf, scoped)
            .stream()
            .map(this::maySetContextAndGet)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<GettableComponent<T>> getAllComponents(final Class<T> type, final Scoped scoped) {
        return registry.getAllComponents(type, scoped);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final ComponentDescriptor<T> descriptor) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final ComponentDescriptor<T> descriptor, final ComponentFactory<T> factory) {
        registry.registerComponent(descriptor, factory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentRegistry setComponentAliasesGenerator(final ComponentAliasesGenerator generator) {
        return setComponentAliasesGenerator(generator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        registry.close();
    }


    private <T> T maySetContextAndGet(final T component) {
        if (component instanceof AzkarraContextAware) {
            ((AzkarraContextAware)component).setAzkarraContext(this.context);
        }
        return component;
    }
}

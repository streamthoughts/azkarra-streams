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
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ContextAwareComponentFactory implements ComponentFactory {

    private final AzkarraContext context;

    private final ComponentFactory factory;

    /**
     * Creates a new {@link ContextAwareComponentFactory} instance.
     *
     * @param context   the {@link AzkarraContext}.
     */
    public ContextAwareComponentFactory(final AzkarraContext context,
                                        final ComponentFactory factory) {
        this.context = context;
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsComponent(final String alias) {
        return factory.containsComponent(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean containsComponent(final String alias, final Qualifier<T> qualifier) {
        return factory.containsComponent(alias, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean containsComponent(final Class<T> type) {
        return factory.containsComponent(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean containsComponent(final Class<T> type, final Qualifier<T> qualifier) {
        return factory.containsComponent(type, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf) {
        return maySetContextAndGet(factory.getComponent(type, conf));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf, final Qualifier<T> qualifier) {
        return maySetContextAndGet(factory.getComponent(type, conf, qualifier));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> GettableComponent<T> getComponent(final Class<T> type, final Qualifier<T> qualifier) {
        return factory.getComponent(type, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final String alias, final Conf conf) {
        return maySetContextAndGet(factory.getComponent(alias, conf));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final String alias, final Conf conf, final Qualifier<T> qualifier) {
        return maySetContextAndGet(factory.getComponent(alias, conf, qualifier));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final String alias, final Conf conf) {
        Collection<T> components = factory.getAllComponents(alias, conf);
        return components.stream().map(this::maySetContextAndGet).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final String alias, final Conf conf, final Qualifier<T> qualifier) {
        Collection<T> components = factory.getAllComponents(alias, conf, qualifier);
        return components.stream().map(this::maySetContextAndGet).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf) {
        Collection<T> components = factory.getAllComponents(type, conf);
        return components.stream().map(this::maySetContextAndGet).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type,
                                              final Conf conf,
                                              final Qualifier<T> qualifier) {
        Collection<T> components = factory.getAllComponents(type, conf);
        return components.stream().map(this::maySetContextAndGet).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<GettableComponent<T>> getAllComponents(final Class<T> type, final Qualifier<T> qualifier) {
        return factory.getAllComponents(type, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final String componentName,
                                      final Class<T> componentClass,
                                      final Supplier<T> supplier,
                                      final ComponentDescriptorModifier... modifiers) {
        factory.registerSingleton(componentName, componentClass, supplier, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final String componentName,
                                      final Class<T> componentClass,
                                      final ComponentDescriptorModifier... modifiers) {
        factory.registerComponent(componentName, componentClass, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerSingleton(final String componentName,
                                      final Class<T> componentClass,
                                      final Supplier<T> singleton,
                                      final ComponentDescriptorModifier... modifiers) {
        factory.registerSingleton(componentName, componentClass, singleton, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerSingleton(final String componentName,
                                      final Class<T> componentClass,
                                      final ComponentDescriptorModifier... modifiers) {
        factory.registerSingleton(componentName, componentClass, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerSingleton(final T singleton) {
        factory.registerSingleton(singleton);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        factory.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type) {
        return factory.findAllDescriptorsByClass(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type,
                                                                            final Qualifier<T> qualifier) {
        return factory.findAllDescriptorsByClass(type, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias) {
        return factory.findAllDescriptorsByAlias(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias,
                                                                            final Qualifier<T> qualifier) {
        return factory.findAllDescriptorsByAlias(alias, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias) {
        return factory.findDescriptorByAlias(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias,
                                                                      final Qualifier<T> qualifier) {
        return factory.findDescriptorByAlias(alias, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type) {
        return factory.findDescriptorByClass(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type,
                                                                      final Qualifier<T> qualifier) {
        return factory.findDescriptorByClass(type, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerDescriptor(final ComponentDescriptor<T> descriptor) {
        factory.registerDescriptor(descriptor);
    }

    private <T> T maySetContextAndGet(final T component) {
        if (component instanceof AzkarraContextAware) {
            ((AzkarraContextAware)component).setAzkarraContext(this.context);
        }
        return component;
    }

}

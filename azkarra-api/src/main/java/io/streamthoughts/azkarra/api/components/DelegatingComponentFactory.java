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

import io.streamthoughts.azkarra.api.components.condition.ComponentConditionalContext;
import io.streamthoughts.azkarra.api.config.Conf;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A delegating {@link ComponentFactory} implementation.
 */
public class DelegatingComponentFactory implements ComponentFactory {

    protected final ComponentFactory factory;

    protected DelegatingComponentFactory(final ComponentFactory factory) {
        this.factory = Objects.requireNonNull(factory, "factory cannot be null");
    }

    @Override
    public boolean containsComponent(final String type) {
        return factory.containsComponent(type);
    }

    @Override
    public <T> boolean containsComponent(final String type,
                                         final Qualifier<T> qualifier) {
        return factory.containsComponent(type, qualifier);
    }

    @Override
    public <T> boolean containsComponent(final Class<T> type) {
        return factory.containsComponent(type);
    }

    @Override
    public <T> boolean containsComponent(final Class<T> type, final Qualifier<T> qualifier) {
        return factory.containsComponent(type, qualifier);
    }

    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf) {
        return factory.getComponent(type, conf);
    }

    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf, final Qualifier<T> qualifier) {
        return factory.getComponent(type, conf, qualifier);
    }

    @Override
    public <T> GettableComponent<T> getComponentProvider(final Class<T> type, final Qualifier<T> qualifier) {
        return factory.getComponentProvider(type, qualifier);
    }

    @Override
    public <T> T getComponent(final String type, final Conf conf) {
        return factory.getComponent(type, conf);
    }

    @Override
    public <T> T getComponent(final String type, final Conf conf, final Qualifier<T> qualifier) {
        return factory.getComponent(type, conf, qualifier);
    }

    @Override
    public <T> Collection<T> getAllComponents(final String type, final Conf conf) {
        return factory.getAllComponents(type, conf);
    }

    @Override
    public <T> Collection<T> getAllComponents(final String type, final Conf conf, final Qualifier<T> qualifier) {
        return factory.getAllComponents(type, conf, qualifier);
    }

    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf) {
        return factory.getAllComponents(type, conf);
    }

    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf,
                                              final Qualifier<T> qualifier) {
        return factory.getAllComponents(type, conf, qualifier);
    }

    @Override
    public <T> Collection<GettableComponent<T>> getAllComponentProviders(final Class<T> type,
                                                                         final Qualifier<T> qualifier) {
        return factory.getAllComponentProviders(type, qualifier);
    }

    @Override
    public void init(final Conf conf) {
        this.factory.init(conf);
    }

    @Override
    public void close() throws IOException {
        factory.close();
    }

    @Override
    public Set<ClassLoader> getAllClassLoaders() {
        return factory.getAllClassLoaders();
    }

    @Override
    public <T> List<T> loadAllServices(final Class<T> type) {
        return factory.loadAllServices(type);
    }

    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type) {
        return factory.findAllDescriptorsByClass(type);
    }

    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type,
                                                                            final Qualifier<T> qualifier) {
        return factory.findAllDescriptorsByClass(type, qualifier);
    }

    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias) {
        return factory.findAllDescriptorsByAlias(alias);
    }

    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias,
                                                                            final Qualifier<T> qualifier) {
        return factory.findAllDescriptorsByAlias(alias, qualifier);
    }

    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias) {
        return factory.findDescriptorByAlias(alias);
    }

    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias,
                                                                      final Qualifier<T> qualifier) {
        return factory.findDescriptorByAlias(alias, qualifier);
    }

    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type) {
        return factory.findDescriptorByClass(type);
    }

    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type,
                                                                      final Qualifier<T> qualifier) {
        return factory.findDescriptorByClass(type, qualifier);
    }

    @Override
    public <T> void registerDescriptor(final ComponentDescriptor<T> descriptor) {
        factory.registerDescriptor(descriptor);
    }

    @Override
    public <T> void registerComponent(final String componentName,
                                      final Class<T> componentClass,
                                      final Supplier<T> supplier,
                                      final ComponentDescriptorModifier... modifiers) {
        factory.registerComponent(componentName, componentClass, supplier, modifiers);
    }

    @Override
    public <T> void registerComponent(final String componentName,
                                      final Class<T> componentClass,
                                      final ComponentDescriptorModifier... modifiers) {
        factory.registerComponent(componentName, componentClass, modifiers);
    }

    @Override
    public <T> void registerSingleton(final String componentName,
                                      final Class<T> componentClass,
                                      final Supplier<T> singleton,
                                      final ComponentDescriptorModifier... modifiers) {
        factory.registerSingleton(componentName, componentClass, singleton, modifiers);
    }

    @Override
    public <T> void registerSingleton(final String componentName,
                                      final Class<T> componentClass,
                                      final ComponentDescriptorModifier... modifiers) {
        factory.registerSingleton(componentName, componentClass, modifiers);
    }

    @Override
    public <T> void registerSingleton(final T singleton) {
        factory.registerSingleton(singleton);
    }

    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type,
                                                                            final ComponentConditionalContext context){
        return factory.findAllDescriptorsByClass(type, context);
    }

    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type,
                                                                            final ComponentConditionalContext context,
                                                                            final Qualifier<T> qualifier) {
        return factory.findAllDescriptorsByClass(type, context, qualifier);
    }

    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias,
                                                                            final ComponentConditionalContext context){
        return factory.findAllDescriptorsByAlias(alias, context);
    }

    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias,
                                                                            final ComponentConditionalContext context,
                                                                            final Qualifier<T> qualifier) {
        return factory.findAllDescriptorsByAlias(alias, context, qualifier);
    }

    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias,
                                                                      final ComponentConditionalContext context) {
        return factory.findDescriptorByAlias(alias, context);
    }

    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias,
                                                                      final ComponentConditionalContext context,
                                                                      final Qualifier<T> qualifier) {
        return factory.findDescriptorByAlias(alias, context, qualifier);
    }

    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type,
                                                                      final ComponentConditionalContext context) {
        return factory.findDescriptorByClass(type, context);
    }

    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type,
                                                                      final ComponentConditionalContext context,
                                                                      final Qualifier<T> qualifier) {
        return factory.findDescriptorByClass(type, context, qualifier);
    }
}

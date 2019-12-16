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
package io.streamthoughts.azkarra.runtime.components;

import io.streamthoughts.azkarra.api.components.ComponentAliasesGenerator;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.components.ComponentRegistryAware;
import io.streamthoughts.azkarra.api.components.NoSuchComponentException;
import io.streamthoughts.azkarra.api.components.NoUniqueComponentException;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The default {@link ComponentRegistry} implementation.
 */
public class DefaultComponentRegistry implements ComponentRegistry {

    protected static final Logger LOG = LoggerFactory.getLogger(DefaultComponentRegistry.class);

    private final Map<Class<?>, List<GettableComponent<?>>> components;

    // Multiple classes with the same FQCN can be loaded using different ClassLoader.
    private final Map<String, List<Class<?>>> componentAliases;

    private ComponentAliasesGenerator aliasesGenerator;

    /**
     * Creates a new {@link DefaultComponentRegistry} instance.
     */
    public DefaultComponentRegistry() {
        componentAliases = new HashMap<>();
        components = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegistered(final String classOrAlias) {
        Objects.requireNonNull(classOrAlias, "classOrAlias cannot be null");
        return componentAliases.containsKey(classOrAlias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegistered(final Class<?> type) {
        Objects.requireNonNull(type, "type cannot be null");
        return componentAliases.containsKey(type.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchedked")
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias) {
        Objects.requireNonNull(alias, "alias cannot be null");
        Optional<GettableComponent<T>> opt = findSingleDescriptorAndFactoryForType(alias);
        return opt.map(GettableComponent::descriptor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchedked")
    public <T> Optional<ComponentDescriptor<T>> findLatestDescriptorByAlias(final String alias) {
        if (!isRegistered(alias)) {
            return Optional.empty();
        }
        final List<GettableComponent<T>> matched = resolveComponentsForClassOrAlias(alias);

        Optional<GettableComponent<T>> latest = matched.stream()
                .sorted()
                .findFirst();

        return latest.map(GettableComponent::descriptor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findLatestDescriptorByAliasAndVersion(final String alias,
                                                                                      final String version) {
        if (!isRegistered(alias)) {
            return Optional.empty();
        }
        final Optional<GettableComponent<T>> component = resolveComponentForAliasAndVersion(alias, version);
        return component.map(GettableComponent::descriptor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorByAlias(final String alias) {
        if (!isRegistered(alias)) {
            throw new NoSuchComponentException("No component registered for class or alias '" + alias + "'.");
        }
        final Class<T>[] classes = resolveTypesForClassOrAlias(alias);
        return Arrays.stream(classes)
          .flatMap(clazz -> findAllDescriptorsByType(clazz).stream())
          .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByType(final Class<T> type) {
        Objects.requireNonNull(type, "type cannot be null");
        return components.getOrDefault(type, Collections.emptyList())
            .stream()
            .map(df -> (ComponentDescriptor<T>) df.descriptor())
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf) {
        Objects.requireNonNull(type, "type cannot be null");
        Optional<GettableComponent<T>> df = findSingleDescriptorAndFactoryForType(type);
        if (df.isPresent()) {
            return df.get().make(conf);
        }
        throw new NoSuchComponentException("No component registered for class '" + type.getName() + "'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getLatestComponent(final Class<T> type, final Conf conf) {
        return resolveLatestComponent((Class<T>[])new Class<?>[]{type}, conf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getLatestComponent(final String classOrAlias, final Conf conf) {
        if (!isRegistered(classOrAlias)) {
            throw new NoSuchComponentException("No component registered for class or alias '" + classOrAlias + "'.");
        }
        return resolveLatestComponent(resolveTypesForClassOrAlias(classOrAlias), conf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getVersionedComponent(final String alias,
                                       final String version,
                                       final Conf conf) {
        if (!isRegistered(alias)) {
            throw new NoSuchComponentException("No component registered for class or alias '" + alias + "'.");
        }

        final Optional<GettableComponent<T>> component = resolveComponentForAliasAndVersion(alias, version);

        if (component.isPresent()) {
            return component.get().make(conf);
        }
        throw new NoSuchComponentException("No component for version '" + version + "'.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final String alias, final Conf conf) {
        Optional<GettableComponent<T>> matched = findSingleDescriptorAndFactoryForType(alias);
        if (matched.isPresent()) {
            return matched.get().make(conf);
        }
        throw new NoSuchComponentException("No component registered for class or alias '" + alias + "'.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf) {
        Objects.requireNonNull(type, "type cannot be null");
        List<GettableComponent<T>> components = resolveAllComponentsForType(type);
        return components.stream().map(gettable -> gettable.make(conf)).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final String classOrAlias, final Conf conf) {
        if (!isRegistered(classOrAlias)) {
            return Collections.emptyList();
        }
        List<GettableComponent<T>> components = resolveComponentsForClassOrAlias(classOrAlias);
        return components.stream().map(gettable -> gettable.make(conf)).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final ComponentDescriptor<T> descriptor) {
        registerComponent(descriptor, new BasicComponentFactory<>(descriptor.type()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final ComponentDescriptor<T> descriptor, final ComponentFactory<T> factory) {
        LOG.info(
            "Registering component descriptor for: type='{}', version='{}'",
            descriptor.className(),
            descriptor.version());

        GettableComponent<T> gettable = new GettableComponent<>(descriptor, factory);
        registerAliasesFor(descriptor);

        ClassUtils
            .getAllSuperTypes(descriptor.type())
            .forEach(cls -> components.computeIfAbsent(cls, k -> new LinkedList<>()).add(gettable));

        if (factory instanceof ComponentRegistryAware) {
            ((ComponentRegistryAware)factory).setRegistry(this);
        }
    }

    private <T> List<GettableComponent<T>> resolveComponentsForClassOrAlias(final String classOrAlias) {
        final Class<T>[] classes = resolveTypesForClassOrAlias(classOrAlias);
        return Arrays
            .stream(classes)
            .flatMap(clazz -> resolveAllComponentsForType(clazz).stream())
            .collect(Collectors.toList());
    }

    private <T> Optional<GettableComponent<T>> resolveComponentForAliasAndVersion(final String alias,
                                                                                  final String version) {
        final List<GettableComponent<T>> matched = resolveComponentsForClassOrAlias(alias);
        return matched.stream()
            .filter(gettable -> gettable.descriptor().isVersioned())
            .filter(gettable -> gettable.descriptor().version().equals(version))
            .findFirst();
    }

    private <T> T resolveLatestComponent(final Class<T>[] classes, final Conf conf) {
        final List<GettableComponent<T>> matched = Arrays
            .stream(classes)
            .flatMap( clazz -> resolveAllComponentsForType(clazz).stream())
            .collect(Collectors.toList());

        Optional<GettableComponent<T>> latest = matched.stream()
                .sorted()
                .findFirst();
        if (latest.isPresent()) {
            GettableComponent<T> gettable = latest.get();
            return gettable.make(conf);
        }
        throw new NoSuchComponentException("No component registered for class '" + classes[0].getName() + "'");
    }

    @SuppressWarnings("unchecked")
    private <T> List<GettableComponent<T>> resolveAllComponentsForType(final Class<T> cls) {
        return components.getOrDefault(cls, Collections.emptyList())
            .stream()
            .map(o -> (GettableComponent<T>)o) // do cast
            .collect(Collectors.toList());
    }

    private <T> Class<T>[] resolveTypesForClassOrAlias(final String classOrAlias) {
        return componentAliases.get(classOrAlias).toArray((Class<T>[])new Class<?>[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultComponentRegistry setComponentAliasesGenerator(final ComponentAliasesGenerator aliasesGenerator) {
        Objects.requireNonNull(aliasesGenerator,  "aliasesGenerator cannot be null");
        this.aliasesGenerator = aliasesGenerator;
        return this;
    }

    private  <T> Optional<GettableComponent<T>> findSingleDescriptorAndFactoryForType(final String classOrAlias) {
        if (!isRegistered(classOrAlias)) {
            return Optional.empty();
        }
        final Class<T>[] classes = resolveTypesForClassOrAlias(classOrAlias);
        if (classes.length > 1) {
            throw new NoUniqueComponentException("Expected single matching component for " +
                "class '" + classes[0].getName() + "' but found " + classes.length);
        }
        return findSingleDescriptorAndFactoryForType(classes[0]);
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<GettableComponent<T>> findSingleDescriptorAndFactoryForType(final Class<T> type) {
        if (!components.containsKey(type)) {
            return Optional.empty();
        }

        List<GettableComponent<?>> matched = components.get(type);
        if (matched.size() > 1) {
            throw new NoUniqueComponentException("Expected single matching component for " +
                "class '" + type.getName() + "' but found " + matched.size());
        }
        return Optional.of((GettableComponent<T>)matched.get(0));
    }

    private void registerAliasesFor(final ComponentDescriptor descriptor) {

        final List<String> aliases = new LinkedList<>();
        aliases.add(descriptor.className());

        if (aliasesGenerator != null) {
            Set<String> computed = aliasesGenerator.getAliasesFor(descriptor, allDescriptors());
            if (!aliases.isEmpty()) {
                LOG.info("Registered aliases '{}' for component {}.", computed, descriptor.className());
                descriptor.addAliases(computed);
                aliases.addAll(computed);
            }
        }
        aliases.forEach(alias -> {
            List<Class<?>> types = componentAliases.computeIfAbsent(alias, key -> new LinkedList<>());
            types.add(descriptor.type());
        });
    }

    private Collection<ComponentDescriptor> allDescriptors() {
        return components.values()
            .stream()
            .flatMap(List::stream)
            .map(GettableComponent::descriptor)
            .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        components.values()
            .stream()
            .flatMap(List::stream)
            .forEach(GettableComponent::close);
    }

    /**
     * Simple class for holding a pair of component descriptor and factory.
     *
     * @param <T>   the component-type.
     */
    private static class GettableComponent<T> implements Comparable<GettableComponent<T>>, Closeable {

        final ComponentFactory<T> factory;
        final ComponentDescriptor<T> descriptor;
        final List<T> instances;

        /**
         * Creates a new {@link GettableComponent} instance.
         *
         * @param descriptor    the {@link ComponentDescriptor} instance.
         * @param factory       the {@link ComponentFactory} instance.
         */
        GettableComponent(final ComponentDescriptor<T> descriptor,
                          final ComponentFactory<T> factory) {
            this.factory = factory;
            this.descriptor = descriptor;
            this.instances = new LinkedList<>();
        }

        ComponentDescriptor<T> descriptor() {
            return descriptor;
        }

        synchronized T make(final Conf conf) {

            if (factory.isSingleton() && !instances.isEmpty()) {
                return instances.get(0);
            }

            final ClassLoader descriptorClassLoader = descriptor.getClassLoader();
            final ClassLoader classLoader = ClassUtils.compareAndSwapLoaders(descriptorClassLoader);
            try {
                Configurable.mayConfigure(factory, conf);

                T instance = factory.make();

                if (descriptor.isCloseable() || factory.isSingleton()) {
                    instances.add(instance);
                }

                Configurable.mayConfigure(instance, conf);

                return instance;
            } finally {
                ClassUtils.compareAndSwapLoaders(classLoader);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            if (!descriptor.isCloseable()) {
                return;
            }
            for (T instance : instances) {
                try {
                    ((Closeable)instance).close();
                } catch (IOException e) {
                    LOG.warn("Error while closing component", e);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final GettableComponent<T> that) {
            return this.descriptor.compareTo(that.descriptor);
        }
    }
}

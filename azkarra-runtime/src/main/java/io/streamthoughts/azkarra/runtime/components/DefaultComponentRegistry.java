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
import io.streamthoughts.azkarra.api.components.GettableComponent;
import io.streamthoughts.azkarra.api.components.NoSuchComponentException;
import io.streamthoughts.azkarra.api.components.NoUniqueComponentException;
import io.streamthoughts.azkarra.api.components.Scoped;
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
    public boolean isRegistered(final Class<?> type, final Scoped scoped) {
        return !resolveAllComponentsForType(type, scoped).isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchedked")
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias) {
        Objects.requireNonNull(alias, "alias cannot be null");
        Optional<GettableComponent<T>> opt = findComponentForType(alias, null);
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
        final List<GettableComponent<T>> matched = resolveComponentsForClassOrAlias(alias, null);

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
        final Optional<GettableComponent<T>> component = resolveComponentForAliasAndVersion(
            alias,
            version,
            null
        );
        return component.map(GettableComponent::descriptor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorByAlias(final String alias) {
        if (!isRegistered(alias)) {
            throw new NoSuchComponentException("No component registered for type '" + alias + "'");
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
    public <T> T getComponent(final Class<T> type, final Conf conf, final Scoped scoped) {
        Objects.requireNonNull(type, "type cannot be null");

        Optional<GettableComponent<T>> df = findComponentForType(type, scoped);
        if (df.isPresent()) {
            return df.get().get(conf);
        }

        throw noSuchComponentException(type.getName(), null, scoped);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> GettableComponent<T> getComponent(final Class<T> type, final Scoped scoped) {
        Objects.requireNonNull(type, "type cannot be null");
        Optional<GettableComponent<T>> df = findComponentForType(type, scoped);
        if (df.isPresent()) {
            return df.get();
        }
        throw noSuchComponentException(type.getName(), null, scoped);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getLatestComponent(final Class<T> type, final Conf conf, final Scoped scoped) {
        return resolveLatestComponent((Class<T>[])new Class<?>[]{type}, conf, scoped);
    }

    @Override
    public <T> T getLatestComponent(final String classOrAlias, final Conf conf, final Scoped scoped) {
        if (!isRegistered(classOrAlias)) {
            throw new NoSuchComponentException("No component registered for type '" + classOrAlias + "'");
        }
        return resolveLatestComponent(resolveTypesForClassOrAlias(classOrAlias), conf, scoped);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getVersionedComponent(final String alias,
                                       final String version,
                                       final Conf conf,
                                       final Scoped scoped) {
        if (!isRegistered(alias)) {
            throw new NoSuchComponentException("No component registered for type '" + alias + "'");
        }

        final Optional<GettableComponent<T>> component = resolveComponentForAliasAndVersion(alias, version, scoped);

        if (component.isPresent()) {
            return component.get().get(conf);
        }
        throw noSuchComponentException(alias, version, scoped);
    }

    private NoSuchComponentException noSuchComponentException(final String alias,
                                                              final String version,
                                                              final Scoped scoped) {
        if (scoped != null && version != null)
            return new NoSuchComponentException(
                "No component registered for type '" + alias + "', version '"
                        + version + "' and scope '" + scoped + "'");

        if (version != null)
            return new NoSuchComponentException(
                "No component registered for type '" + alias + "', version '" + version + "'");

        if (scoped != null)
            return new NoSuchComponentException(
                "No component registered for type '" + alias + "', scoped '" + scoped + "'");

        return new NoSuchComponentException(
                "No component registered for type '" + alias + "'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final String alias, final Conf conf, final Scoped scoped) {
        Optional<GettableComponent<T>> matched = findComponentForType(alias, scoped);
        if (matched.isPresent()) {
            return matched.get().get(conf);
        }

        throw noSuchComponentException(alias, null, scoped);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf, final Scoped scoped) {
        Objects.requireNonNull(type, "type cannot be null");
        List<GettableComponent<T>> components = resolveAllComponentsForType(type, scoped);
        return components.stream().map(gettable -> gettable.get(conf)).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<GettableComponent<T>> getAllComponents(Class<T> type, Scoped scoped) {
        Objects.requireNonNull(type, "type cannot be null");
        return resolveAllComponentsForType(type, scoped);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final String classOrAlias, final Conf conf, final Scoped scoped) {
        if (!isRegistered(classOrAlias)) {
            return Collections.emptyList();
        }
        List<GettableComponent<T>> components = resolveComponentsForClassOrAlias(classOrAlias, scoped);
        return components.stream().map(gettable -> gettable.get(conf)).collect(Collectors.toList());
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

        InternalGettableComponent<T> gettable = new InternalGettableComponent<>(descriptor, factory);
        registerAliasesFor(descriptor);

        ClassUtils
            .getAllSuperTypes(descriptor.type())
            .forEach(cls -> components.computeIfAbsent(cls, k -> new LinkedList<>()).add(gettable));

        if (factory instanceof ComponentRegistryAware) {
            ((ComponentRegistryAware)factory).setRegistry(this);
        }
    }

    private <T> List<GettableComponent<T>> resolveComponentsForClassOrAlias(final String classOrAlias,
                                                                            final Scoped scoped) {
        final Class<T>[] classes = resolveTypesForClassOrAlias(classOrAlias);
        return Arrays
            .stream(classes)
            .flatMap(clazz -> resolveAllComponentsForType(clazz, scoped).stream())
            .collect(Collectors.toList());
    }

    private <T> Optional<GettableComponent<T>> resolveComponentForAliasAndVersion(final String alias,
                                                                                  final String version,
                                                                                  final Scoped scoped) {
        final List<GettableComponent<T>> matched = resolveComponentsForClassOrAlias(alias, scoped);
        return matched.stream()
            .filter(gettable -> gettable.descriptor().isVersioned())
            .filter(gettable -> gettable.descriptor().version().equals(version))
            .findFirst();
    }

    private <T> T resolveLatestComponent(final Class<T>[] classes, final Conf conf, final Scoped scoped) {
        final List<GettableComponent<T>> matched = Arrays
            .stream(classes)
            .flatMap( clazz -> resolveAllComponentsForType(clazz, scoped).stream())
            .collect(Collectors.toList());

        Optional<GettableComponent<T>> latest = matched.stream()
                .sorted()
                .findFirst();
        if (latest.isPresent()) {
            GettableComponent<T> gettable = latest.get();
            return gettable.get(conf);
        }
        throw noSuchComponentException(classes[0].getName(), null, scoped);
    }

    @SuppressWarnings("unchecked")
    private <T> List<GettableComponent<T>> resolveAllComponentsForType(final Class<T> cls, final Scoped scoped) {
        List<GettableComponent<T>> components = this.components.getOrDefault(cls, Collections.emptyList())
                .stream()
                .map(o -> (GettableComponent<T>) o) // do cast
                .collect(Collectors.toList());
        if (scoped == null) {
           return components;
        }

        return components.stream()
                .filter(g -> g.descriptor().hasScope(scoped))
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

    private  <T> Optional<GettableComponent<T>> findComponentForType(final String type,
                                                                     final Scoped scoped) {
        if (!isRegistered(type)) {
            return Optional.empty();
        }
        final Class<T>[] classes = resolveTypesForClassOrAlias(type);
        if (classes.length > 1) {
            throw new NoUniqueComponentException("Expected single matching component for " +
                "class '" + classes[0].getName() + "' but found " + classes.length);
        }
        return findComponentForType(classes[0], scoped);
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<GettableComponent<T>> findComponentForType(final Class<T> type,
                                                                    final Scoped scoped) {
        if (!components.containsKey(type)) {
            return Optional.empty();
        }

        List<GettableComponent<?>> matched = components.get(type);

        if (scoped != null) {
            matched = matched.stream()
                .filter(gettable -> gettable.descriptor().hasScope(scoped))
                .collect(Collectors.toList());
        }

        if (matched.isEmpty()) {
            return Optional.empty();
        }

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
    private static class InternalGettableComponent<T> implements Comparable<InternalGettableComponent<T>>,
            GettableComponent<T>,
            Closeable {

        private static final Logger LOG = LoggerFactory.getLogger(InternalGettableComponent.class);

        private final ComponentFactory<T> factory;
        private final ComponentDescriptor<T> descriptor;
        private final List<T> instances;

        /**
         * Creates a new {@link InternalGettableComponent} instance.
         *
         * @param descriptor    the {@link ComponentDescriptor} instance.
         * @param factory       the {@link ComponentFactory} instance.
         */
        InternalGettableComponent(final ComponentDescriptor<T> descriptor, final ComponentFactory<T> factory) {
            this.factory = factory;
            this.descriptor = descriptor;
            this.instances = new LinkedList<>();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized T get(final Conf conf) {

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

        public ComponentDescriptor<T> descriptor() {
            return descriptor;
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
        public int compareTo(final InternalGettableComponent<T> that) {
            return this.descriptor.compareTo(that.descriptor);
        }
    }

}

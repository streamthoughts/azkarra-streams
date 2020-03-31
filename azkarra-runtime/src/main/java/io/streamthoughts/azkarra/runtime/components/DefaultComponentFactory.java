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
import io.streamthoughts.azkarra.api.components.ComponentDescriptorFactory;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorModifier;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentFactoryAware;
import io.streamthoughts.azkarra.api.components.ComponentRegistrationException;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.components.ConflictingBeanDefinitionException;
import io.streamthoughts.azkarra.api.components.GettableComponent;
import io.streamthoughts.azkarra.api.components.NoSuchComponentException;
import io.streamthoughts.azkarra.api.components.NoUniqueComponentException;
import io.streamthoughts.azkarra.api.components.Qualifier;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The default {@link ComponentRegistry} implementation.
 */
public class DefaultComponentFactory implements ComponentFactory {

    protected static final Logger LOG = LoggerFactory.getLogger(DefaultComponentFactory.class);

    private final Map<ComponentKey, GettableComponent> componentObjects;

    private final Map<Class, List<ComponentDescriptor>> descriptorsByType;

    // Multiple classes with the same FQCN can be loaded using different ClassLoader.
    private final Map<String, List<Class>> componentTypesByAlias;

    private ComponentAliasesGenerator componentAliasesGenerator;

    private ComponentDescriptorFactory descriptorFactory;

    /**
     * Creates a new {@link DefaultComponentFactory} instance.
     */
    public DefaultComponentFactory(final ComponentDescriptorFactory descriptorFactory) {
        Objects.requireNonNull(descriptorFactory, "descriptorFactory cannot be null");
        this.descriptorFactory = descriptorFactory;
        this.componentTypesByAlias = new HashMap<>();
        this.componentObjects = new HashMap<>();
        this.descriptorsByType = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsComponent(final String alias) {
        Objects.requireNonNull(alias, "alias cannot be null");
        return componentTypesByAlias.containsKey(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean containsComponent(final String type, final Qualifier<T> qualifier) {
        return !findAllDescriptorsByAlias(type, qualifier).isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean containsComponent(final Class<T> type) {
        return descriptorsByType.containsKey(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean containsComponent(final Class<T> type, final Qualifier<T> qualifier) {
        return !findAllDescriptorsByClass(type, qualifier).isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf) {
        Optional<ComponentDescriptor<T>> optional = findDescriptorByClass(type);
        if (optional.isEmpty())
            throw new NoSuchComponentException("No component registered for type '" + type.getName() + "'");

        ComponentDescriptor<T> descriptor = optional.get();
        return getComponent(descriptor).get(conf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf, final Qualifier<T> qualifier) {
        Optional<ComponentDescriptor<T>> optional = findDescriptorByClass(type, qualifier);
        if (optional.isEmpty())
            throw new NoSuchComponentException("No component registered for type '" + type.getName() + "'");

        ComponentDescriptor<T> descriptor = optional.get();
        return getComponent(descriptor).get(conf);
    }

    @SuppressWarnings("unchecked")
    private <T> GettableComponent<T> getComponent(final ComponentDescriptor<T> descriptor) {
        return componentObjects.get(getComponentKey(descriptor));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type) {
        Objects.requireNonNull(type, "type cannot be null");
        return findDescriptorCandidatesByType(type).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type,
                                                                            final Qualifier<T> qualifier) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(qualifier, "qualifier cannot be null");
        Stream<ComponentDescriptor<T>> candidates = findDescriptorCandidatesByType(type);
        return qualifier.filter(type, candidates).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> GettableComponent<T> getComponent(final Class<T> type, final Qualifier<T> qualifier) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(qualifier, "qualifier cannot be null");

        Optional<ComponentDescriptor<T>> optional = findDescriptorByClass(type, qualifier);
        if (optional.isEmpty())
            throw new NoSuchComponentException("No component registered for type '" + type + "'");

        ComponentDescriptor<T> descriptor = optional.get();
        return getComponent(descriptor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final String alias, final Conf conf) {
        Optional<ComponentDescriptor<T>> optional = findDescriptorByAlias(alias);
        if (optional.isEmpty())
            throw new NoSuchComponentException("No component registered for type '" + alias + "'");

        ComponentDescriptor<T> descriptor = optional.get();
        return getComponent(descriptor).get(conf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final String alias, final Conf conf, final Qualifier<T> qualifier) {
        Optional<ComponentDescriptor<T>> optional = findUniqueDescriptor(
            alias,
            findDescriptorByAlias(alias, qualifier).stream()
        );

        if (optional.isEmpty())
            throw new NoSuchComponentException("No component registered for type '" + alias + "'");

        ComponentDescriptor<T> descriptor = optional.get();
        return getComponent(descriptor).get(conf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final String alias, final Conf conf) {
        return getAllComponents(alias, conf, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf, final Qualifier<T> qualifier) {
        Objects.requireNonNull(type, "type cannot be null");
        return getAllComponents(type, qualifier)
            .stream()
            .map(gettable -> gettable.get(conf))
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<GettableComponent<T>> getAllComponents(final Class<T> type, final Qualifier<T> qualifier) {
        Collection<ComponentDescriptor<T>> descriptors = findAllDescriptorsByClass(type, qualifier);
        return descriptors.stream().map(this::getComponent).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final String alias,
                                              final Conf conf,
                                              final Qualifier<T> qualifier) {
        Collection<ComponentDescriptor<T>> descriptors = findAllDescriptorsByAlias(alias, qualifier);
        return descriptors.stream()
            .map(this::getComponent)
            .map(gettable -> gettable.get(conf))
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf) {
        Collection<ComponentDescriptor<T>> descriptors = findAllDescriptorsByClass(type);
        return descriptors.stream()
            .map(this::getComponent)
            .map(gettable -> gettable.get(conf))
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        LOG.info("Closing all registered components");
        componentObjects.values().forEach(GettableComponent::close);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias) {
        return findAllDescriptorsByAlias(alias, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias,
                                                                            final Qualifier<T> qualifier) {
        Objects.requireNonNull(alias, "alias cannot be null");
        List<Class> types = componentTypesByAlias.get(alias);
        if (types == null)
            return Collections.emptyList();

        Stream<ComponentDescriptor<T>> candidates = types.stream().flatMap(type -> {
            Stream<ComponentDescriptor<T>> stream = findDescriptorCandidatesByType(type);
            return (qualifier == null) ? stream : qualifier.filter(type, stream);
        });
        return candidates.sorted(ComponentDescriptor.ORDER_BY_ORDER).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias) {
        return findDescriptorByAlias(alias, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias,
                                                                      final Qualifier<T> qualifier) {
        return findUniqueDescriptor(alias, findAllDescriptorsByAlias(alias, qualifier).stream());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type) {
        Stream<ComponentDescriptor<T>> candidates = findDescriptorCandidatesByType(type);
        return findUniqueDescriptor(type.getName(), candidates);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type,
                                                                      final Qualifier<T> qualifier) {
        Stream<ComponentDescriptor<T>> candidates = findDescriptorCandidatesByType(type);
        Stream<ComponentDescriptor<T>> filtered = qualifier.filter(type, candidates);
        return findUniqueDescriptor(type.getName(), filtered);
    }

    private <T> Optional<ComponentDescriptor<T>> findUniqueDescriptor(final String type,
                                                                      final Stream<ComponentDescriptor<T>> candidates) {
        List<ComponentDescriptor<T>> descriptors = candidates.collect(Collectors.toList());
        if (descriptors.size() > 1) {
            final int many = descriptors.size();
            descriptors = Qualifiers
                .<T>byPrimary()
                .filter(null, descriptors.stream())
                .collect(Collectors.toList());
            if (descriptors.size() != 1)
                throw new NoUniqueComponentException("Expected single matching component for " +
                        "type '" + type + "' but found " + many);
        }


        if (descriptors.isEmpty())
            return Optional.empty();

        return Optional.of(descriptors.get(0));
    }

    @SuppressWarnings("unchecked")
    private <T> Stream<ComponentDescriptor<T>> findDescriptorCandidatesByType(final Class<T> type) {
        List<ComponentDescriptor> candidates = descriptorsByType.getOrDefault(type, Collections.emptyList());
        return candidates.stream()
            .map(d -> (ComponentDescriptor<T>)d)
            .sorted(ComponentDescriptor.ORDER_BY_ORDER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final String componentName,
                                      final Class<T> componentClass,
                                      final Supplier<T> supplier,
                                      final ComponentDescriptorModifier... modifiers) {
        Objects.requireNonNull(componentClass, "componentClass can't be null");
        Objects.requireNonNull(supplier, "supplier can't be null");
        ComponentDescriptor<T> descriptor = descriptorFactory.make(
            componentName,
            componentClass,
            supplier,
            false
        );
        for (ComponentDescriptorModifier modifier : modifiers) {
            descriptor = modifier.apply(descriptor);
        }
        registerDescriptor(descriptor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final String componentName,
                                      final Class<T> componentClass,
                                      final ComponentDescriptorModifier... modifiers) {
        Objects.requireNonNull(componentClass, "componentClass can't be null");
        final BasicComponentFactory<T> supplier = new BasicComponentFactory<>(
            componentClass,
            componentClass.getClassLoader()
        );
        registerComponent(componentName, componentClass, supplier, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerSingleton(final String componentName,
                                      final Class<T> componentClass,
                                      final Supplier<T> singleton,
                                      final ComponentDescriptorModifier... modifiers) {
        Objects.requireNonNull(componentClass, "componentClass can't be null");
        Objects.requireNonNull(singleton, "singleton can't be null");
        ComponentDescriptor<T> descriptor = descriptorFactory.make(
            componentName,
            componentClass,
            singleton,
            true
        );
        for (ComponentDescriptorModifier modifier : modifiers) {
            descriptor = modifier.apply(descriptor);
        }

        registerDescriptor(descriptor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerSingleton(final String componentName,
                                      final Class<T> componentClass,
                                      final ComponentDescriptorModifier... modifiers) {
        Objects.requireNonNull(componentClass, "componentClass can't be null");
        final BasicComponentFactory<T> singleton = new BasicComponentFactory<>(
            componentClass,
            componentClass.getClassLoader()
        );
        registerSingleton(componentName, componentClass, singleton, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> void registerSingleton(final T singleton) {
        Objects.requireNonNull(singleton, "singleton can't be null");
        registerSingleton((Class<T>)singleton.getClass(), () -> singleton);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerDescriptor(final ComponentDescriptor<T> descriptor) {
        Objects.requireNonNull(descriptor, "descriptor can't be null");
        LOG.info("Registering component descriptor for name='{}', type='{}', version='{}'",
                descriptor.name(),
                descriptor.type(),
                descriptor.version());

        if (descriptor.name() == null)
            throw new ComponentRegistrationException("Can't register component with name 'null': " + descriptor);
        if (descriptor.type() == null)
            throw new ComponentRegistrationException("Can't register component with type 'null': " + descriptor);

        registerAliasesFor(descriptor);

        ClassUtils.getAllSuperTypes(descriptor.type()).forEach(cls ->
            descriptorsByType.computeIfAbsent(cls, k -> new LinkedList<>()).add(descriptor)
        );

        final ComponentKey<T> key = getComponentKey(descriptor);

        if (componentObjects.put(key, new InternalGettableComponent<>(descriptor)) != null) {
            throw new ConflictingBeanDefinitionException(
                "Failed to resister ComponentDescriptor, component already exists for key: " + key);
        }
    }

    public DefaultComponentFactory setComponentAliasesGenerator(final ComponentAliasesGenerator aliasesGenerator) {
        Objects.requireNonNull(aliasesGenerator,  "aliasesGenerator cannot be null");
        this.componentAliasesGenerator = aliasesGenerator;
        return this;
    }

    private void registerAliasesFor(final ComponentDescriptor descriptor) {

        final List<String> aliases = new LinkedList<>();
        aliases.add(descriptor.className());

        if (componentAliasesGenerator != null) {
            Set<String> computed = componentAliasesGenerator.getAliasesFor(descriptor, descriptors());
            if (!aliases.isEmpty()) {
                LOG.info("Registered aliases '{}' for component {}.", computed, descriptor.className());
                descriptor.addAliases(computed);
                aliases.addAll(computed);
            }
        }
        aliases.forEach(alias -> {
            List<Class> types = componentTypesByAlias.computeIfAbsent(alias, key -> new LinkedList<>());
            types.add(descriptor.type());
        });
    }

    private Collection<ComponentDescriptor> descriptors() {
        return descriptorsByType.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private <T> ComponentKey<T> getComponentKey(final ComponentDescriptor<T> descriptor) {
        Qualifier<T> qualifier = Qualifiers.byName(descriptor.name());
        if (descriptor.isVersioned())
            qualifier = Qualifiers.byQualifiers(qualifier, Qualifiers.byVersion(descriptor.version()));

        return new ComponentKey<>(descriptor.type(), qualifier);
    }

    /**
     * Simple class for holding a pair of component descriptor and factory.
     *
     * @param <T>   the component-type.
     */
    private class InternalGettableComponent<T> implements Comparable<InternalGettableComponent<T>>,
            GettableComponent<T>,
            Closeable {

        private final ComponentDescriptor<T> descriptor;
        private final List<T> instances;

        /**
         * Creates a new {@link InternalGettableComponent} instance.
         *
         * @param descriptor    the {@link ComponentDescriptor} instance.
         */
        InternalGettableComponent(final ComponentDescriptor<T> descriptor) {
            this.descriptor = descriptor;
            this.instances = new LinkedList<>();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized T get(final Conf conf) {

            if (descriptor.isSingleton() && !instances.isEmpty()) {
                return instances.get(0);
            }

            final ClassLoader descriptorClassLoader = descriptor.classLoader();
            final ClassLoader classLoader = ClassUtils.compareAndSwapLoaders(descriptorClassLoader);
            try {
                Supplier<T> factory = descriptor.supplier();

                if (factory instanceof ComponentFactoryAware) {
                    ((ComponentFactoryAware)factory).setComponentFactory(DefaultComponentFactory.this);
                }

                Configurable.mayConfigure(factory, conf);

                T instance = factory.get();

                if (descriptor.isCloseable() || descriptor.isSingleton()) {
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

    private static class ComponentKey<T> {

        private final Class<T> componentType;
        private final Qualifier<T> qualifier;

        private final int hashCode;

        ComponentKey(final Class<T> componentType,
                     final Qualifier<T> qualifier) {
            this.componentType = componentType;
            this.qualifier = qualifier;

            hashCode = Objects.hash(componentType, qualifier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ComponentKey)) return false;
            ComponentKey<?> that = (ComponentKey<?>) o;
            return Objects.equals(componentType, that.componentType) &&
                    Objects.equals(qualifier, that.qualifier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return hashCode;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "[" +
                    "type=" + componentType.getName() +
                    ", qualifier=" + qualifier +
                    ']';
        }
    }
}

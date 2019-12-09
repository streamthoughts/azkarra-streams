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

import io.streamthoughts.azkarra.api.annotations.Singleton;
import io.streamthoughts.azkarra.api.components.ComponentClassReader;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorFactory;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.components.Versioned;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Simple implementation for {@link ComponentClassReader} interface.
 */
public class DefaultProviderClassReader implements ComponentClassReader {

    private final Logger LOG = LoggerFactory.getLogger(DefaultComponentRegistry.class);

    private final Map<Class<?>, ComponentDescriptorFactory<?>> providerFactories;

    /**
     * Creates a new {@link DefaultProviderClassReader} instance.
     */
    public DefaultProviderClassReader() {
        this.providerFactories = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void addDescriptorFactoryForType(final Class<T> type,
                                                final ComponentDescriptorFactory<T> factory) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(factory, "factory cannot be null");
        providerFactories.put(type, factory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerComponent(final String componentClassName, final ComponentRegistry registry) {
        Objects.requireNonNull(componentClassName, "componentClassName cannot be null");
        Objects.requireNonNull(registry, "registry cannot be registry");
        try {
            final Class<?> cls = Class.forName(componentClassName);
            registerComponent(cls, registry);
        } catch (ClassNotFoundException e) {
            throw new AzkarraException("Failed to register class '" + componentClassName + "'", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final Class<T> componentClass, final ComponentRegistry registry) {
        registerComponent(componentClass, registry, componentClass.getClassLoader());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final Class<T> componentClass,
                                      final ComponentRegistry registry,
                                      final ClassLoader classLoader) {
        Objects.requireNonNull(componentClass, "componentClass cannot be null");
        Objects.requireNonNull(registry, "registry cannot be registry");

        boolean isSingleton = ClassUtils.isSuperTypesAnnotatedWith(componentClass, Singleton.class);
        registerComponent(new BasicComponentFactory<>(componentClass, isSingleton), registry, classLoader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void registerComponent(final ComponentFactory<T> factory,
                                      final ComponentRegistry registry) {
        registerComponent(factory, registry, factory.getClass().getClassLoader());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> void registerComponent(final ComponentFactory<T> factory,
                                      final ComponentRegistry registry,
                                      final ClassLoader classLoader) {
        Objects.requireNonNull(factory, "factory cannot be null");
        Objects.requireNonNull(registry, "registry cannot be registry");

        final Class<T> type = factory.getType();

        if (ClassUtils.canBeInstantiated(type)) {
            ComponentDescriptorFactory<T> descriptorFactory = (ComponentDescriptorFactory<T>)findFactoryForType(type);

            ComponentDescriptor<T> descriptor;

            final String version = getVersionFor(type, classLoader);
            if (descriptorFactory != null) {
                descriptor = descriptorFactory.make(type, version, classLoader);
            } else {
                descriptor = new ComponentDescriptor<>(type, classLoader, version);
            }
            registry.registerComponent(descriptor, factory);
        } else {
            LOG.debug("Skipping {} as it is not concrete implementation", type);
        }
    }

    private ComponentDescriptorFactory<?> findFactoryForType(final Class<?> type) {
        ComponentDescriptorFactory<?> factory = providerFactories.get(type);
        if (factory != null)
            return factory;
        for (Map.Entry<Class<?>, ComponentDescriptorFactory<?>> entry :
                providerFactories.entrySet()) {
            try {
                type.asSubclass(entry.getKey());
                providerFactories.put(type, entry.getValue());
                return entry.getValue();
            } catch (final ClassCastException e) {
                // Expected, ignore
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String getVersionFor(final Class<?> cls, final ClassLoader classLoader) {
        ClassLoader saveLoader = ClassUtils.compareAndSwapLoaders(classLoader);
        try {
            return Versioned.class.isAssignableFrom(cls) ?
                ClassUtils.newInstance((Class<Versioned>) cls).version() : null;
        } finally {
            ClassUtils.compareAndSwapLoaders(saveLoader);
        }
    }
}

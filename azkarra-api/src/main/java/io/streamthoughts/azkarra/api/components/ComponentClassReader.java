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

import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.providers.Provider;

/**
 * The {@link ComponentClassReader} serves as a proxy interface for registering components to {@link ComponentRegistry}.
 *
 * @see ComponentRegistry
 * @see ComponentDescriptor
 * @see ComponentDescriptorFactory
 *
 */
public interface ComponentClassReader {

    /**
     * Add a descriptor factory to this reader.
     *
     * @param type      the type
     * @param factory   the {@link ComponentDescriptorFactory} instance.
     */
    <T> void addDescriptorFactoryForType(final Class<T> type,
                                         final ComponentDescriptorFactory<T> factory);
    /**
     * Registers the {@link Provider} class using the specified {@link ComponentRegistry}.
     *
     * @param componentClassName the component class name to be read and register.
     * @param registry           the {@link ComponentRegistry} instance to be used.
     *
     * @throws                  {@link AzkarraException} if the specified class cannot be found.
     */
    void registerComponent(final String componentClassName, final ComponentRegistry registry);

    /**
     * Registers the {@link Provider} class using the specified {@link ComponentRegistry}.
     *
     * @param componentClass the component class to be read and register.
     * @param registry      the {@link ComponentRegistry} instance to be used.
     * @param <T>           the component-type.
     * @throws              {@link AzkarraException} if the specified class cannot be read.
     */
    <T>  void registerComponent(final Class<T> componentClass,
                                final ComponentRegistry registry);

    /**
     * Registers the {@link Provider} class using the specified {@link ComponentRegistry}.
     *
     * @param componentClass the component class to be read and register.
     * @param registry       the {@link ComponentRegistry} instance to be used.
     * @param classLoader    the {@link ClassLoader} instance used to load the component.
     * @param <T>            the component-type.
     * @throws              {@link AzkarraException} if the specified class cannot be read.
     */
    <T>  void registerComponent(final Class<T> componentClass,
                                final ComponentRegistry registry,
                                final ClassLoader classLoader);

    /**
     * Registers the {@link Provider} class using the specified {@link ComponentRegistry}.
     *
     * @param componentFactory  the {@link ComponentFactory} instance to be used.
     * @param registry          the {@link ComponentRegistry} instance to be used.
     * @param <T>               the component-type.
     * @throws                  {@link AzkarraException} if the specified class cannot be read.
     */
    <T> void registerComponent(final ComponentFactory<T> componentFactory,
                               final ComponentRegistry registry);

    /**
     * Registers the {@link Provider} class using the specified {@link ComponentRegistry}.
     *
     * @param componentFactory  the {@link ComponentFactory} instance to be used.
     * @param registry          the {@link ComponentRegistry} instance to be used.
     * @param classLoader       the {@link ClassLoader} instance used to load the component.
     * @param <T>               the component-type.
     * @throws                  {@link AzkarraException} if the specified class cannot be read.
     */
    <T> void registerComponent(final ComponentFactory<T> componentFactory,
                               final ComponentRegistry registry,
                               final ClassLoader classLoader);
}

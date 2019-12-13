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

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;

import java.io.Closeable;
import java.util.Collection;
import java.util.Optional;

/**
 * The {@link ComponentRegistry} is the main interface for managing components used in an Azkarra application.
 */
public interface ComponentRegistry extends Closeable {

    /**
     * Checks whether the specified components class or alias is already registered.
     *
     * @param alias  the fully qualified class name or an alias of the component.
     * @return              {@code true} if a provider exist, {code false} otherwise.
     */
    boolean isRegistered(final String alias);

    /**
     * Finds a {@link ComponentDescriptor} for the specified class or alias.
     *
     * @param alias  the fully qualified class name or an alias of the component.
     * @param <T>    the component type.
     *
     * @return       the optional {@link ComponentDescriptor} instance.
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     */
    <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias);

    /**
     * Finds a {@link ComponentDescriptor} for the specified class or alias.
     * If more than one component is registered for the given alias, the latest version is returned.
     *
     * @param alias  the fully qualified class name or an alias of the component.
     * @param <T>    the component type.
     *
     * @return       the optional {@link ComponentDescriptor} instance.
     */
    <T> Optional<ComponentDescriptor<T>> findLatestDescriptorByAlias(final String alias);

    /**
     * Finds a {@link ComponentDescriptor} for the specified class or alias.
     *
     * @param alias    the fully qualified class name or an alias of the component.
     * @param version  the version of the component.
     * @param <T>      the component type.
     *
     * @return         the optional {@link ComponentDescriptor} instance.
     */
    <T> Optional<ComponentDescriptor<T>> findLatestDescriptorByAliasAndVersion(final String alias,
                                                                               final String version);

    /**
     * Finds a {@link ComponentDescriptor} for the specified class or alias.
     *
     * @param alias  the fully qualified class name or an alias of the component.
     * @param <T>    the component type.
     *
     * @return       the optional {@link ComponentDescriptor} instance.
     *
     */
    <T> Collection<ComponentDescriptor<T>> findAllDescriptorByAlias(final String alias);

    /**
     * Finds all registered providers for the specified type.
     *
     * @param type      the component class.
     * @param <T>       the component type.
     * @return          the collection of {@link ComponentDescriptor}.
     */
    <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByType(final Class<T> type);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param type      the component class.
     * @param conf      the configuration used if the component implement {@link Configurable}.
     * @param <T>       the component-type.
     *
     * @return          the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    <T> T getComponent(final Class<T> type, final Conf conf);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     * If more than one component is registered for the given type, the latest version is returned.
     *
     * @param type      the component class.
     * @param conf      the configuration used if the component implement {@link Configurable}.
     * @param <T>       the component-type.
     *
     * @return          the instance of type {@link T}.
     *
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    <T> T getLatestComponent(final Class<T> type, final Conf conf);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param alias  the fully qualified class name or an alias of the component.
     * @param conf   the configuration used if the component implement {@link Configurable}.
     * @param <T>    the component-type.
     *
     * @return       the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given class or alias..
     */
    <T> T getComponent(final String alias, final Conf conf);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     * If more than one component is registered for the given type, the latest version is returned.
     *
     * @param alias  the fully qualified class name or an alias of the component.
     * @param conf   the configuration used if the component implement {@link Configurable}.
     * @param <T>    the component-type.
     *
     * @return       the instance of type {@link T}.
     *
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    <T> T getLatestComponent(final String alias, final Conf conf);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     * If more than one component is registered for the given type, the latest version is returned.
     *
     * @param alias  the fully qualified class name or an alias of the component.
     * @param conf   the configuration used if the component implement {@link Configurable}.
     * @param <T>    the component-type.
     *
     * @return       the instance of type {@link T}.
     *
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    <T> T getVersionedComponent(final String alias, final String version, final Conf conf);

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * @param alias  the fully qualified class name or an alias of the component.
     * @param conf   the configuration used if the component implement {@link Configurable}.
     * @param <T>    the component-type.
     *
     * @return       the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given class or alias..
     */
    <T> Collection<T> getAllComponents(final String alias, final Conf conf);

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * @param type    the component class.
     * @param conf    the configuration used if the component implement {@link Configurable}.
     * @param <T>     the component-type.
     *
     * @return        the instance of type {@link T}.
     */
    <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf);

    /**
     * Registers the specified {@link ComponentDescriptor} to this {@link ComponentRegistry}.
     *
     * @param descriptor    the {@link ComponentDescriptor} instance to be registered.
     * @param <T>           the component type.
     */
    <T> void registerComponent(final ComponentDescriptor<T> descriptor);

    /**
     * Registers the specified {@link ComponentDescriptor} to this {@link ComponentRegistry}.
     *
     * @param descriptor    the {@link ComponentDescriptor} instance to be registered.
     * @param factory       the {@link ComponentFactory} instance.
     * @param <T>           the component type.
     */
    <T> void registerComponent(final ComponentDescriptor<T> descriptor, final ComponentFactory<T> factory);

    /**
     * Sets the {@link ComponentAliasesGenerator} instance.
     *
     * @param generator the {@link ComponentAliasesGenerator} used for generating provider aliases.
     */
    ComponentRegistry setComponentAliasesGenerator(final ComponentAliasesGenerator generator);
}

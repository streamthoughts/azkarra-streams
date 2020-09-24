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
package io.streamthoughts.azkarra.api.components;

import java.util.Collection;
import java.util.Optional;

public interface ComponentDescriptorRegistry {

    /**
     * Finds all {@link ComponentDescriptor} registered for the specified type.
     *
     * @param type      the component class.
     * @param <T>       the component type.
     *
     * @return          the collection of {@link ComponentDescriptor}.
     */
    <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type);

    /**
     * Finds all {@link ComponentDescriptor} registered for the specified type.
     *
     * @param type      the component class.
     * @param <T>       the component type.
     *
     * @return          the collection of {@link ComponentDescriptor}.
     */
    <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByClass(final Class<T> type,
                                                                     final Qualifier<T> qualifier);

    /**
     * Finds all {@link ComponentDescriptor} registered for the specified alias.
     *
     * @param alias     the fully qualified class name or an alias of the component.
     * @param <T>       the component type.
     *
     * @return          the collection of {@link ComponentDescriptor}.
     *
     */
    <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias);

    /**
     * Finds all {@link ComponentDescriptor} registered for the specified alias.
     *
     * @param alias     the fully qualified class name or an alias of the component.
     * @param <T>       the component type.
     *
     * @return          the collection of {@link ComponentDescriptor}.
     *
     */
    <T> Collection<ComponentDescriptor<T>> findAllDescriptorsByAlias(final String alias,
                                                                     final Qualifier<T> qualifier);

    /**
     * Finds a {@link ComponentDescriptor} for the specified type.
     *
     * @param alias      the fully qualified class name or an alias of the component.
     * @param <T>        the component type.
     *
     * @return           the optional {@link ComponentDescriptor} instance.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     *
     */
    <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias) ;

    /**
     * Finds a {@link ComponentDescriptor} for the specified type and options.
     *
     * @param alias      the fully qualified class name or an alias of the component.
     * @param qualifier  the options used to qualified the component.
     * @param <T>        the component type.
     *
     * @return           the optional {@link ComponentDescriptor} instance.
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     */
    <T> Optional<ComponentDescriptor<T>> findDescriptorByAlias(final String alias,
                                                               final Qualifier<T> qualifier);

    /**
     * Finds a {@link ComponentDescriptor} for the specified type.
     *
     * @param type       the component class.
     * @param <T>        the component type.
     *
     * @return           the optional {@link ComponentDescriptor} instance.
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     */
    <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type);

    /**
     * Finds a {@link ComponentDescriptor} for the specified type and options.
     *
     * @param type       the fully qualified class name or an alias of the component.
     * @param qualifier  the options used to qualified the component.
     * @param <T>        the component type.
     *
     * @return           the optional {@link ComponentDescriptor} instance.
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     */
    <T> Optional<ComponentDescriptor<T>> findDescriptorByClass(final Class<T> type,
                                                               final Qualifier<T> qualifier);

    /**
     * Registers the specified {@link ComponentDescriptor} to this {@link ComponentRegistry}.
     *
     * @param descriptor    the {@link ComponentDescriptor} instance to be registered.
     * @param <T>           the component type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    <T> void registerDescriptor(final ComponentDescriptor<T> descriptor);
}

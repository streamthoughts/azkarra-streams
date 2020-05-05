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

import java.util.function.Supplier;

/**
 * The {@link ComponentRegistry} is the main interface for managing components used in an Azkarra application.
 */
public interface ComponentRegistry {

    /**
     * Registers the component supplier for the specified type.
     *
     * @param componentClass    the class-type of the component.
     * @param supplier          the supplier of the component.
     * @param modifiers         the component descriptor modifiers.
     * @param <T>               the component-type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    default <T> void registerComponent(final Class<T> componentClass,
                                       final Supplier<T> supplier,
                                       final ComponentDescriptorModifier... modifiers) {
        registerComponent(null, componentClass, supplier, modifiers);
    }

    /**
     * Registers the component supplier for the specified type and name.
     *
     * @param componentName     the name of the component.
     * @param componentClass    the class-type of the component.
     * @param supplier          the supplier of the component.
     * @param modifiers         the component descriptor modifiers.
     * @param <T>               the component-type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    <T> void registerComponent(final String componentName,
                               final Class<T> componentClass,
                               final Supplier<T> supplier,
                               final ComponentDescriptorModifier... modifiers);

    /**
     * Registers the component supplier for the specified type.
     *
     * @param componentClass    the class-type of the component.
     * @param <T>               the component-type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    default <T> void registerComponent(final Class<T> componentClass,
                                       final ComponentDescriptorModifier... modifiers) {
        registerComponent(null, componentClass, modifiers);
    }

    /**
     * Registers the component supplier for the specified type and name.
     *
     * @param componentName     the name of the component.
     * @param componentClass    the class-type of the component.
     * @param <T>               the component-type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    <T> void registerComponent(final String componentName,
                               final Class<T> componentClass,
                               final ComponentDescriptorModifier... modifiers);

    /**
     * Registers the component supplier for the specified type and name.
     * The given supplier will be used for creating a shared instance of type {@link T}.
     *
     * @param componentClass    the class-type of the component.
     * @param singleton         the supplier of the component.
     * @param <T>               the component-type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    default <T> void registerSingleton(final Class<T> componentClass,
                                       final Supplier<T> singleton,
                                       final ComponentDescriptorModifier... modifiers) {
        registerSingleton(null, componentClass, singleton, modifiers);
    }

    /**
     * Registers the component supplier for the specified type and name.
     * The given supplier will be used for creating a shared instance of type {@link T}.
     *
     * @param componentName     the name of the component.
     * @param componentClass    the class-type of the component.
     * @param singleton         the supplier of the component.
     * @param <T>               the component-type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    <T> void registerSingleton(final String componentName,
                               final Class<T> componentClass,
                               final Supplier<T> singleton,
                               final ComponentDescriptorModifier... modifiers);

    /**
     * Registers a singleton no-arg constructor component supplier for the specified type.
     *
     * @param componentClass    the class-type of the component.
     * @param <T>               the component-type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    default <T> void registerSingleton(final Class<T> componentClass,
                                       final ComponentDescriptorModifier... modifiers) {
        registerSingleton(null, componentClass, modifiers);
    }

    /**
     * Registers a singleton no-arg constructor component supplier for the specified type.
     *
     * @param componentName     the name of the component.
     * @param componentClass    the class-type of the component.
     * @param <T>               the component-type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    <T> void registerSingleton(final String componentName,
                               final Class<T> componentClass,
                               final ComponentDescriptorModifier... modifiers);

    /**
     * Register the specified shared instance.
     *
     * @param singleton the singleton component instance.
     * @param <T>       the component-type.
     *
     * @throws ConflictingComponentDefinitionException if a component is already register for that descriptor.
     */
    <T> void registerSingleton(final T singleton);
}

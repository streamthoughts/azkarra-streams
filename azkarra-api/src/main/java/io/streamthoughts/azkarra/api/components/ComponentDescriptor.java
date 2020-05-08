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

import io.streamthoughts.azkarra.api.components.condition.Condition;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.util.Version;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Describes a single component managed by a {@link ComponentFactory}.
 *
 * @param <T>   the component type.
 */
public interface ComponentDescriptor<T> extends Ordered {

    Comparator<ComponentDescriptor<?>> ORDER_BY_VERSION = (c1, c2) -> {
        if (!c1.isVersioned()) return 1;
        else if (!c2.isVersioned()) return -1;
        else return c1.version().compareTo(c2.version());
    };

    Comparator<ComponentDescriptor<?>> ORDER_BY_ORDER = Comparator.comparingInt(Ordered::order);

    /**
     * Gets the name of the component.
     *
     * @return  the name, or {@code null} if the name is not set.
     */
    String name();

    /**
     * Gets the component metadata.
     *
     * @return  the {@link ComponentMetadata}.
     */
    ComponentMetadata metadata();

    /**
     * Gets the classloader used to load the component.
     *
     * @return  the {@link ClassLoader}.
     */
    ClassLoader classLoader();

    /**
     * Adds new aliases to reference the described component.
     *
     * @param aliases   the aliases to be added.
     */
    void addAliases(final Set<String> aliases);

    /**
     * Gets the set of aliases for this component.
     *
     * @return  the aliases.
     */
    Set<String> aliases();

    /**
     * Gets the name of the describe component.
     *
     * @return  the name.
     */
    default String className() {
        return type().getName();
    }

    /**
     * Gets the version of the described component.
     *
     * @return  the component version if versioned, otherwise {@code null}.
     */
    Version version();

    /**
     * Gets the supplier used to create a new component of type {@link T}.
     *
     * @return  the {@link Supplier}.
     */
    Supplier<T> supplier();

    /**
     * Checks whether the described component has a valid versioned.
     *
     * @return  {@code true } if versioned, otherwise {@code false}.
     */
    default boolean isVersioned() {
        return version() != null;
    }

    /**
     * Gets the type of the described component.
     *
     * @return  the class of type {@code T}.
     */
    Class<T> type();

    /**
     * Checks if the described component implement {@link Closeable}.
     *
     * @return  {@code true } if closeable, otherwise {@code false}.
     */
    default boolean isCloseable() {
        return AutoCloseable.class.isAssignableFrom(type());
    }

    /**
     * Checks if the described component is a singleton.
     *
     * @return {@code true } if is singleton, otherwise {@code false}.
     */
    boolean isSingleton();

    /**
     * Checks if the described component is the primary component
     * that must be selected in the case of multiple possible implementations.
     *
     * @see Qualifiers#byPrimary()
     *
     * @return {@code true} if is primary, otherwise {@code false}.
     */
    boolean isPrimary();

    /**
     * Checks if the described component is a secondary component that
     * must be de-prioritize in the case of multiple possible implementations.
     *
     * @see Qualifiers#bySecondary()
     * @see Qualifiers#excludeSecondary()
     *
     * @return {@code true} if is secondary, otherwise {@code false}.
     */
    boolean isSecondary();

    /**
     * Checks if the described component should be create and configure eagerly.
     *
     * @see io.streamthoughts.azkarra.api.annotations.Eager
     *
     * @return {@code true} if it s an eager component, otherwise {@code false}.
     */
    boolean isEager();

    /**
     * Gets the {@link Condition}  that need to be fulfilled for
     * this component to be eligible for use in the application.
     *
     * @return  the {@link Condition}s.
     */
    Optional<Condition> condition();
}

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

import io.streamthoughts.azkarra.api.util.Version;

import java.io.Closeable;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The {@link ComponentDescriptor} is the base class used fro describing components.
 *
 * @param <T>   the component-type.
 */
public class ComponentDescriptor<T> implements Comparable<ComponentDescriptor<T>> {

    private static final String UNKNOWN_VERSION = "UNKNOWN";

    private final String name;

    private final String version;

    private final Version comparableVersion;

    private final Class<T> type;

    private final Set<String> aliases;

    private final ClassLoader classLoader;

    /**
     * Creates a new {@link ComponentDescriptor} instance.
     *
     * @param type   the component class.
     */
    public ComponentDescriptor(final Class<T> type) {
        this(type, type.getClassLoader(), null);
    }

    /**
     * Creates a new {@link ComponentDescriptor} instance.
     *
     * @param type        the component class (cannot be{@code null}).
     * @param version     the component version (may be {@code null}).
     */
    public ComponentDescriptor(final Class<T> type,
                               final String version) {
        this(type, type.getClassLoader(), version);
    }

    /**
     * Creates a new {@link ComponentDescriptor} instance.
     *
     * @param type        the component class (cannot be{@code null}).
     * @param classLoader the {@link ClassLoader} from which the component is loaded.
     * @param version     the component version (may be {@code null}).
     */
    public ComponentDescriptor(final Class<T> type,
                               final ClassLoader classLoader,
                               final String version) {
        Objects.requireNonNull(type, "type can't be null");
        this.name = type.getName();
        this.version = version;
        this.comparableVersion = version != null ? Version.parse(version) : null;
        this.type = type;
        this.classLoader = classLoader;
        this.aliases = new TreeSet<>();
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Adds new aliases to reference the described component.
     *
     * @param aliases   the aliases to be added.
     */
    public void addAliases(final Set<String> aliases) {
        this.aliases.addAll(aliases);
    }

    /**
     * Gets the set of aliases for this component.
     *
     * @return  the aliases.
     */
    public Set<String> aliases() {
        return aliases;
    }

    /**
     * Gets the name of the describe component.
     *
     * @return  the name.
     */
    public String className() {
        return name;
    }

    /**
     * Gets the version of the described component.
     *
     * @return  the component version if versioned, otherwise {@link ComponentDescriptor#UNKNOWN_VERSION}.
     */
    public String version() {
        return version == null ? UNKNOWN_VERSION : version.toString();
    }

    /**
     * Checks whether the described component has a valid versioned.
     *
     * @return  {@code true } if versioned, otherwise {@code false}.
     */
    public boolean isVersioned() {
        return version != null;
    }

    /**
     * Gets the type of the described component.
     *
     * @return  the class of type {@code T}.
     */
    public Class<T> type() {
        return type;
    }

    /**
     * Checks whether the describe component implement {@link Closeable}.
     *
     * @return  {@code true } if closeable, otherwise {@code false}.
     */
    public boolean isCloseable() {
        return Closeable.class.isAssignableFrom(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final ComponentDescriptor<T> that) {
        if (!this.isVersioned()) return 1;
        else if (!that.isVersioned()) return -1;
        else return this.comparableVersion.compareTo(that.comparableVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ComponentDescriptor{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", comparableVersion=" + comparableVersion +
                ", type=" + type +
                ", aliases=" + aliases +
                '}';
    }
}

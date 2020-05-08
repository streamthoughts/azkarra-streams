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
import io.streamthoughts.azkarra.api.util.Version;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * The {@link SimpleComponentDescriptor} is the base class used fro describing components.
 *
 * @param <T>   the component-type.
 */
public class SimpleComponentDescriptor<T> implements ComponentDescriptor<T> {

    private String name;

    private final Version version;

    private final Class<T> type;

    private final Supplier<T> supplier;

    private final Set<String> aliases;

    private ClassLoader classLoader;

    private ComponentMetadata metadata;

    private final boolean isSingleton;

    private final boolean isPrimary;

    private final boolean isSecondary;

    private final Condition condition;

    private final boolean isConditional;

    private final int order;

    /**
     * Creates a new {@link SimpleComponentDescriptor} instance.
     *
     * @param name          the name of the component.
     * @param type          the type of the component.
     * @param supplier      the supplier of the component.
     * @param isSingleton   is the component singleton.
     */
    public SimpleComponentDescriptor(final String name,
                                     final Class<T> type,
                                     final Supplier<T> supplier,
                                     final boolean isSingleton) {
        this(
            name,
            type,
            type.getClassLoader(),
            supplier,
            null,
            isSingleton,
            false,
            false,
            null,
            Ordered.LOWEST_ORDER
        );
    }

    /**
     * Creates a new {@link SimpleComponentDescriptor} instance.
     *
     * @param name          the name of the component.
     * @param type          the type of the component.
     * @param supplier      the supplier of the component.
     * @param version       the version of the component.
     * @param isSingleton   is the component singleton.
     */
    public SimpleComponentDescriptor(final String name,
                                     final Class<T> type,
                                     final Supplier<T> supplier,
                                     final String version,
                                     final boolean isSingleton) {
        this(
            name,
            type,
            type.getClassLoader(),
            supplier,
            version,
            isSingleton,
            false,
            false,
            null,
            Ordered.LOWEST_ORDER
        );
    }

    /**
     * Creates a new {@link SimpleComponentDescriptor} instance.
     *
     * @param name          the name of the component.
     * @param type          the type of the component.
     * @param classLoader   the component classloader.
     * @param supplier      the supplier of the component.
     * @param version       the version of the component.
     * @param isSingleton   is the component singleton.
     */
    public SimpleComponentDescriptor(final String name,
                                     final Class<T> type,
                                     final ClassLoader classLoader,
                                     final Supplier<T> supplier,
                                     final String version,
                                     final boolean isSingleton,
                                     final boolean isPrimary,
                                     final boolean isSecondary,
                                     final Condition condition,
                                     final int order) {
        Objects.requireNonNull(type, "type can't be null");
        Objects.requireNonNull(supplier, "supplier can't be null");
        this.name = name;
        this.version = version != null ? Version.parse(version) : null;
        this.supplier = supplier;
        this.type = type;
        this.classLoader = classLoader == null ? type.getClassLoader() : classLoader;
        this.aliases = new TreeSet<>();
        this.metadata = new ComponentMetadata();
        this.isSingleton = isSingleton;
        this.isPrimary = isPrimary;
        this.isSecondary = isSecondary;
        this.isConditional = condition != null;
        this.condition = condition;
        this.order = order;
    }

    /**
     * Creates a new {@link SimpleComponentDescriptor} instance from the given one.
     *
     * @param descriptor    the {@link SimpleComponentDescriptor} to copy.
     */
    protected SimpleComponentDescriptor(final ComponentDescriptor<T> descriptor) {
        this(
            descriptor.name(),
            descriptor.type(),
            descriptor.classLoader(),
            descriptor.supplier(),
            descriptor.version().toString(),
            descriptor.isSingleton(),
            descriptor.isPrimary(),
            descriptor.isSecondary(),
            descriptor.condition().orElse(null),
            descriptor.order()
        );
        metadata = descriptor.metadata();
        aliases.addAll(descriptor.aliases());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentMetadata metadata() {
        return metadata;
    }

    public void metadata(final ComponentMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader classLoader() {
        return classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAliases(final Set<String> aliases) {
        this.aliases.addAll(aliases);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> aliases() {
        return aliases;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String className() {
        return type.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version version() {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Supplier<T> supplier() {
        return supplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> type() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleton()  {
        return isSingleton;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPrimary() {
        return isPrimary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecondary() {
        return isSecondary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Condition> condition() {
        return Optional.ofNullable(condition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int order() {
        return order;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleComponentDescriptor)) return false;
        SimpleComponentDescriptor<?> that = (SimpleComponentDescriptor<?>) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(type, that.type) &&
                Objects.equals(supplier, that.supplier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, version, type, supplier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[" +
                "name=" + name +
                ", version=" + version +
                ", type=" + type +
                ", aliases=" + aliases +
                ", isSingleton=" + isSingleton +
                ", isPrimary=" + isPrimary +
                ", isSecondary=" + isSecondary +
                ", isConditional=" + isConditional +
                ", metadata=" + metadata +
                ", order=" + order +
                ']';
    }
}

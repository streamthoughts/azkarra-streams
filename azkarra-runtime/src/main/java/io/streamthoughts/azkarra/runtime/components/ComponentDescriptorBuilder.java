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
package io.streamthoughts.azkarra.runtime.components;

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentMetadata;
import io.streamthoughts.azkarra.api.components.condition.Condition;
import io.streamthoughts.azkarra.api.components.SimpleComponentDescriptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.util.Version;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ComponentDescriptorBuilder<T> implements ComponentDescriptor<T> {

    private String name;
    private ComponentMetadata metadata;
    private Class<T> type;
    private ClassLoader classLoader;
    private String version;
    private Supplier<T> supplier;
    private boolean isSingleton;
    private boolean isPrimary;
    private boolean isSecondary;
    private Condition condition;
    private boolean isEager = false;
    private Set<String> aliases = new HashSet<>();
    private int order;
    private Conf configuration;

    /**
     * Static helper method for creating a new {@link ComponentDescriptorBuilder} instance.
     *
     * @param <T>   the component type.
     * @return      the new {@link ComponentDescriptorBuilder} instance.
     */
    public static <T> ComponentDescriptorBuilder<T> create() {
        return new ComponentDescriptorBuilder<>();
    }

    /**
     * Static helper method for creating a new {@link ComponentDescriptorBuilder} instance.
     *
     * @param descriptor the {@link ComponentDescriptor} instance.
     * @param <T>        the component type.
     * @return           the new {@link ComponentDescriptorBuilder} instance.
     */
    public static <T> ComponentDescriptorBuilder<T> create(final ComponentDescriptor<T> descriptor) {
        return new ComponentDescriptorBuilder<T>()
            .name(descriptor.name())
            .version(Optional.ofNullable(descriptor.version()).map(Object::toString).orElse(null))
            .type(descriptor.type())
            .metadata(descriptor.metadata())
            .classLoader(descriptor.classLoader())
            .supplier(descriptor.supplier())
            .isSingleton(descriptor.isSingleton())
            .order(descriptor.order())
            .isPrimary(descriptor.isPrimary())
            .isSecondary(descriptor.isSecondary())
            .condition(descriptor.condition().orElse(null))
            .configuration(descriptor.configuration())
            .isEager(descriptor.isEager());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Sets the name of the component.
     *
     * @param name      the name of the component.
     * @return          {@code this}
     */
    public ComponentDescriptorBuilder<T> name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentMetadata metadata() {
        return metadata;
    }

    /**
     * Sets the metadata of the component.
     *
     * @param metadata  the metadata of the component.
     * @return          {@code this}
     */
    public ComponentDescriptorBuilder<T> metadata(final ComponentMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader classLoader() {
        return classLoader;
    }

    /**
     * Sets the classLoader of the component.
     *
     * @param classLoader  the classLoader of the component.
     * @return             {@code this}
     */
    public ComponentDescriptorBuilder<T> classLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
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
    public Version version() {
        return version != null ? Version.parse(version) : null;
    }

    /**
     * Sets the version of the component.
     *
     * @param version  the version of the component.
     * @return         {@code this}
     */
    public ComponentDescriptorBuilder<T> version(final String version) {
        this.version = version;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Supplier<T> supplier() {
        return supplier;
    }

    /**
     * Sets the supplier of the component.
     *
     * @param supplier  the supplier of the component.
     * @return          {@code this}
     */
    public ComponentDescriptorBuilder<T> supplier(final Supplier<T> supplier) {
        this.supplier = supplier;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> type() {
        return type;
    }

    /**
     * Sets the type of the component.
     *
     * @param type  the type of the component.
     * @return      {@code this}
     */
    public ComponentDescriptorBuilder<T> type(final Class<T> type) {
        this.type = type;
        return this;
    }

    public ComponentDescriptorBuilder<T> isSingleton(final boolean isSingleton) {
        this.isSingleton = isSingleton;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleton() {
        return isSingleton;
    }

    public ComponentDescriptorBuilder<T> isPrimary(final boolean isPrimary) {
        this.isPrimary = isPrimary;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPrimary() {
        return isPrimary;
    }

    public ComponentDescriptorBuilder<T> isSecondary(final boolean isSecondary) {
        this.isSecondary = isSecondary;
        return this;
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
    public boolean isEager() {
        return isEager;
    }

    public ComponentDescriptorBuilder<T> isEager(final boolean isEager) {
        this.isEager = isEager;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Condition> condition() {
        return Optional.ofNullable(condition);
    }

    public ComponentDescriptorBuilder<T> condition(final Condition condition) {
        this.condition = condition;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf configuration() {
        return Optional.ofNullable(configuration).orElse(Conf.empty());
    }

    public ComponentDescriptorBuilder<T> configuration(final Conf configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Sets the order of the component.
     *
     * @param order the order of the component.
     * @return      {@code this}
     */
    public ComponentDescriptorBuilder<T> order(int order) {
        this.order = order;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int order() {
        return order;
    }

    public ComponentDescriptor<T> build() {
        SimpleComponentDescriptor<T> descriptor = new SimpleComponentDescriptor<>(
            name,
            type,
            classLoader,
            supplier,
            version,
            isSingleton,
            isPrimary,
            isSecondary,
            isEager,
            condition,
            order
        );
        descriptor.metadata(metadata);
        descriptor.addAliases(aliases);
        descriptor.configuration(configuration);
        return descriptor;
    }
}


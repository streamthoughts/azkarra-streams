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

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class ContextAwareComponentFactory extends DelegatingComponentFactory {

    private final AzkarraContext context;

    /**
     * Creates a new {@link ContextAwareComponentFactory} instance.
     *
     * @param context   the {@link AzkarraContext}.
     */
    public ContextAwareComponentFactory(final AzkarraContext context,
                                        final ComponentFactory factory) {
        super(factory);
        this.context = Objects.requireNonNull(context, "context cannot be null");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf) {
        return maySetContextAndGet(factory.getComponent(type, conf));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final Class<T> type, final Conf conf, final Qualifier<T> qualifier) {
        return maySetContextAndGet(factory.getComponent(type, conf, qualifier));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final String alias, final Conf conf) {
        return maySetContextAndGet(factory.getComponent(alias, conf));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getComponent(final String alias, final Conf conf, final Qualifier<T> qualifier) {
        return maySetContextAndGet(factory.getComponent(alias, conf, qualifier));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final String alias, final Conf conf) {
        Collection<T> components = factory.getAllComponents(alias, conf);
        return components.stream().map(this::maySetContextAndGet).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final String alias, final Conf conf, final Qualifier<T> qualifier) {
        Collection<T> components = factory.getAllComponents(alias, conf, qualifier);
        return components.stream().map(this::maySetContextAndGet).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf) {
        Collection<T> components = factory.getAllComponents(type, conf);
        return components.stream().map(this::maySetContextAndGet).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getAllComponents(final Class<T> type,
                                              final Conf conf,
                                              final Qualifier<T> qualifier) {
        Collection<T> components = factory.getAllComponents(type, conf);
        return components.stream().map(this::maySetContextAndGet).collect(Collectors.toList());
    }

    private <T> T maySetContextAndGet(final T component) {
        if (component instanceof AzkarraContextAware) {
            ((AzkarraContextAware)component).setAzkarraContext(this.context);
        }
        return component;
    }

}

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

import java.util.Collection;
import java.util.Objects;

public abstract class ComponentModule<T> implements ComponentFactory<T>, ComponentRegistryAware, Configurable {

    private final Class<T> type;
    private Conf configuration;
    private ComponentRegistry registry;

    /**
     * Creates a new {@link ComponentModule} instance.
     *
     * @param type  the component type.
     */
    protected ComponentModule(final Class<T> type) {
        Objects.requireNonNull(type, "type cannot be null");
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRegistry(final ComponentRegistry registry) {
        this.registry = registry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        this.configuration = configuration;
    }

    protected Conf configuration() {
        return configuration;
    }

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param type      the component class.
     * @param <C>       the component-type.
     *
     * @return          the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    protected <C> C getComponentForType(final Class<C> type) {
        return registry.getComponent(type, configuration);
    }

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param classOrAlias  the fully qualified class name or an alias of the component.
     * @param <C>           the component-type.
     *
     * @return              the instance of type {@link C}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given class or alias..
     */
    protected <C> C getComponentForClassOrAlias(final String classOrAlias) {
        return registry.getComponent(classOrAlias, configuration);
    }

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * @param type          the component class.
     * @param <C>           the component-type.
     *
     * @return              the instance of type {@link C}.
     */
    protected <C> Collection<C> getAllComponentsForType(final Class<C> type) {
        return registry.getAllComponents(type, configuration);
    }
}

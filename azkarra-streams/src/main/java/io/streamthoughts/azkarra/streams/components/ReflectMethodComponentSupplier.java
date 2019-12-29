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
package io.streamthoughts.azkarra.streams.components;

import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentFactoryAware;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.errors.AzkarraException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class ReflectMethodComponentSupplier implements Supplier<Object>, Configurable, ComponentFactoryAware {

    private final Object target;
    private final Method factory;

    /**
     * Creates a new {@link ReflectMethodComponentSupplier} instance.
     *
     * @param target    the target object.
     * @param factory   the factory method.
     */
    ReflectMethodComponentSupplier(final Object target, final Method factory) {
        this.target = target;
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get() {
        try {
            return factory.invoke(target);
        } catch (IllegalAccessException e) {
            throw new AzkarraException(e);
        } catch (InvocationTargetException e) {
            throw new AzkarraException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComponentFactory(final ComponentFactory factory) {
        if (ComponentFactoryAware.class.isAssignableFrom(target.getClass())) {
            ((ComponentFactoryAware)target).setComponentFactory(factory);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        Configurable.mayConfigure(target, configuration);
    }
}

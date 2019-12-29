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
package io.streamthoughts.azkarra.runtime.components;

import io.streamthoughts.azkarra.api.util.ClassUtils;

import java.util.Objects;
import java.util.function.Supplier;

public class BasicComponentFactory<T> implements Supplier<T> {

    private final Class<T> componentClass;
    private final ClassLoader classLoader;

    /**
     * Creates a new {@link BasicComponentFactory} instance.
     *
     * @param componentClass  the component class.
     */
    public BasicComponentFactory(final Class<T> componentClass) {
        this(componentClass, componentClass.getClassLoader());
    }

    /**
     * Creates a new {@link BasicComponentFactory} instance.
     *
     * @param componentClass  the component class.
     */
    public BasicComponentFactory(final Class<T> componentClass,
                                 final ClassLoader classLoader) {
        this.componentClass = Objects.requireNonNull(componentClass, "componentClass cannot be null");
        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get() {
        if (classLoader == null)
            return ClassUtils.newInstance(componentClass);
        return ClassUtils.newInstance(componentClass, classLoader);
    }
}
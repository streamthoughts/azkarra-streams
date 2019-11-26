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

import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.errors.AzkarraException;

public class BasicComponentFactory<T> implements ComponentFactory<T> {

    private final Class<T> type;

    private final boolean isSingleton;

    /**
     * Creates a new {@link BasicComponentFactory} instance.
     *
     * @param type  the component type.
     */
    public BasicComponentFactory(final Class<T> type) {
        this(type, false);
    }

    /**
     * Creates a new {@link BasicComponentFactory} instance.
     *
     * @param type  the component type.
     * @param type  is this component a singleton.
     */
    public BasicComponentFactory(final Class<T> type, final boolean isSingleton) {
        this.type = type;
        this.isSingleton = isSingleton;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T make() {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AzkarraException("Cannot create new instance of type '" + getClass().getSimpleName() + "'", e);
        }
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
    public boolean isSingleton() {
        return isSingleton;
    }
}
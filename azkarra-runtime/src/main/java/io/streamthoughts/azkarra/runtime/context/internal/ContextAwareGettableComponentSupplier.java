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
package io.streamthoughts.azkarra.runtime.context.internal;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.components.GettableComponent;
import io.streamthoughts.azkarra.api.config.Conf;

/**
 * Supplier class which is used to get a specific component from {@link AzkarraContext}.
 *
 * @param <T>   the component-type.
 */
public class ContextAwareGettableComponentSupplier<T> extends ContextAwareComponentSupplier<T> {

    private final GettableComponent<T> gettable;

    /**
     * Creates a new {@link ContextAwareGettableComponentSupplier} instance.
     *
     * @param context   the {@link AzkarraContext} instance.
     * @param gettable  the {@link GettableComponent} instance.
     */
    public ContextAwareGettableComponentSupplier(final AzkarraContext context,
                                                 final GettableComponent<T> gettable) {
        super(context);
        this.gettable = gettable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get(final Conf configs) {
        return gettable.get(configs);
    }
}

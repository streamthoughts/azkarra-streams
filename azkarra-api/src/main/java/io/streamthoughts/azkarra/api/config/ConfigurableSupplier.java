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
package io.streamthoughts.azkarra.api.config;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A configurable supplier.
 *
 * @param <T>   the type of the supply object.
 */
public abstract class ConfigurableSupplier<T> implements Supplier<T>, Configurable {

    private Conf config;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        this.config = Objects.requireNonNull(configuration, "configuration cannot be null");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get() {
        verifyState();
        return get(config);
    }

    private void verifyState() {
        if (config == null) throw new IllegalStateException("supplier is not configured");
    }

    public abstract T get(final Conf configs);
}
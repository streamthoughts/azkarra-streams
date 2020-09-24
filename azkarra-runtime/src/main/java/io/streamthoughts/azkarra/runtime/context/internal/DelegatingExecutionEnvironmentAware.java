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
package io.streamthoughts.azkarra.runtime.context.internal;

import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentAware;

import java.util.Objects;

/**
 * A delegating {@link StreamsExecutionEnvironmentAware} implementation.
 */
public class DelegatingExecutionEnvironmentAware<T> implements StreamsExecutionEnvironmentAware {

    final T delegate;

    /**
     * Creates a new {@link DelegatingExecutionEnvironmentAware} instance.
     *
     * @param delegate  the delegate component.
     */
    DelegatingExecutionEnvironmentAware(final T delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExecutionEnvironment(final StreamsExecutionEnvironment environment) {
        if (delegate instanceof StreamsExecutionEnvironmentAware) {
            ((StreamsExecutionEnvironmentAware)delegate).setExecutionEnvironment(environment);
        }
    }
}

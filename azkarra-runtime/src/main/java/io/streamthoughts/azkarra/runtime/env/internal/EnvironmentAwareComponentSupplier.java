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
package io.streamthoughts.azkarra.runtime.env.internal;

import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentAware;
import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Objects;
import java.util.function.Supplier;

import static io.streamthoughts.azkarra.api.config.Configurable.mayConfigure;

public class EnvironmentAwareComponentSupplier<T> {

    private final Supplier<T> componentSupplier;

    /**
     * Creates a new {@link EnvironmentAwareComponentSupplier} instance.
     *
     * @param componentSupplier the component.
     */
    public EnvironmentAwareComponentSupplier(final Supplier<T> componentSupplier) {
        Objects.requireNonNull(componentSupplier, "component cannot be null");
        this.componentSupplier = componentSupplier;
    }

    public T get(final StreamsExecutionEnvironment environment, final Conf componentConf) {
        mayConfigure(componentSupplier, componentConf);
        T component = componentSupplier.get();
        mayConfigure(component, componentConf);
        maySetStreamsExecutionEnvironmentAware(component, environment);
        return component;
    }

    private void maySetStreamsExecutionEnvironmentAware(final Object component,
                                                        final StreamsExecutionEnvironment environment) {
        if (StreamsExecutionEnvironmentAware.class.isAssignableFrom(component.getClass())) {
            ((StreamsExecutionEnvironmentAware)component).setExecutionEnvironment(environment);
        }
    }
}

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

package io.streamthoughts.azkarra.runtime.components.condition;

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.condition.ComponentConditionalContext;
import io.streamthoughts.azkarra.api.components.condition.Condition;
import io.streamthoughts.azkarra.api.components.condition.ConditionContext;
import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link ComponentConditionalContext} implementation..
 */
public class ConfigConditionalContext<T> implements ComponentConditionalContext<ComponentDescriptor<T>> {

    static <T> ConfigConditionalContext<T> of(final Conf config) {
        return new ConfigConditionalContext<>(config);
    }

    private final Conf config;

    /**
     * Creates a new {@link ConfigConditionalContext} instance.
     *
     * @param config    the {@link Conf}.
     */
    public ConfigConditionalContext(final Conf config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnable(final ComponentFactory factory,
                            final ComponentDescriptor<T> descriptor) {
        Objects.requireNonNull(factory, "factory must not be null");
        Objects.requireNonNull(descriptor, "descriptor must not be null");

        final Optional<Condition> optional = descriptor.condition();
        return optional
            .map(condition -> condition.matches(buildConditionContent(factory, descriptor)))
            .orElse(true);
    }

    private ConditionContext<ComponentDescriptor> buildConditionContent(final ComponentFactory factory,
                                                                        final ComponentDescriptor descriptor) {
        return new ConditionContext<>() {
            @Override
            public ComponentFactory getComponentFactory() {
                return factory;
            }

            @Override
            public Conf getConfig() {
                return config;
            }

            @Override
            public ComponentDescriptor getComponent() {
                return descriptor;
            }
        };
    }
}

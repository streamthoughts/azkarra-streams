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
package io.streamthoughts.azkarra.runtime.components;

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorModifier;
import io.streamthoughts.azkarra.api.components.condition.Condition;
import io.streamthoughts.azkarra.api.components.condition.Conditions;
import io.streamthoughts.azkarra.api.config.Conf;

import java.util.List;

public class ComponentDescriptorModifiers {

    /**
     * Gets a modifier implementation that will set a component as eager.
     *
     * @return  a new {@link ComponentDescriptorModifier} instance.
     */
    public static ComponentDescriptorModifier asEager() {
        return new ComponentDescriptorModifier() {
            @Override
            public <T> ComponentDescriptor<T> apply(final ComponentDescriptor<T> descriptor) {
                return ComponentDescriptorBuilder.<T>create(descriptor)
                        .isEager(true)
                        .build();
            }
        };
    }

    /**
     * Gets a modifier implementation that will set a component as primary.
     *
     * @return  a new {@link ComponentDescriptorModifier} instance.
     */
    public static ComponentDescriptorModifier asPrimary() {
        return new ComponentDescriptorModifier() {
            @Override
            public <T> ComponentDescriptor<T> apply(final ComponentDescriptor<T> descriptor) {
                return ComponentDescriptorBuilder.<T>create(descriptor)
                    .isPrimary(true)
                    .build();
            }
        };
    }

    /**
     * Gets a modifier implementation that will set a component as secondary.
     *
     * @return  a new {@link ComponentDescriptorModifier} instance.
     */
    public static ComponentDescriptorModifier asSecondary() {
        return new ComponentDescriptorModifier() {
            @Override
            public <T> ComponentDescriptor<T> apply(final ComponentDescriptor<T> descriptor) {
                return ComponentDescriptorBuilder.<T>create(descriptor)
                    .isSecondary(true)
                    .build();
            }
        };
    }

    /**
     * Gets a modifier implementation that will set the given order of the component.
     *
     * @param order the component order to set.
     * @return      a new {@link ComponentDescriptorModifier} instance.
     */
    public static ComponentDescriptorModifier withOrder(final int order) {
        return new ComponentDescriptorModifier() {
            @Override
            public <T> ComponentDescriptor<T> apply(final ComponentDescriptor<T> descriptor) {
                return ComponentDescriptorBuilder.<T>create(descriptor)
                    .order(order)
                    .build();
            }
        };
    }

    /**
     * Gets a modifier implementation that will set the conditions that need to be
     * fulfilled for the component to be eligible for use in the application.
     *
     * @param conditions the {@link Condition}s to add.
     * @return           a new {@link ComponentDescriptorModifier} instance.
     */
    public static ComponentDescriptorModifier withConditions(final Condition... conditions) {
        return withConditions(List.of(conditions));
    }

    /**
     * Gets a modifier implementation that will set the conditions that need to be
     * fulfilled for the component to be eligible for use in the application.
     *
     * @param conditions the {@link Condition}s to add.
     * @return           a new {@link ComponentDescriptorModifier} instance.
     */
    public static ComponentDescriptorModifier withConditions(final List<Condition> conditions) {
        return new ComponentDescriptorModifier() {
            @Override
            public <T> ComponentDescriptor<T> apply(final ComponentDescriptor<T> descriptor) {
                return ComponentDescriptorBuilder.<T>create(descriptor)
                    .condition(Conditions.compose(conditions))
                    .build();
            }
        };
    }

    /**
     * Gets a modifier implementation that will set the default configuration to
     * pass to the component if it implements the {@link io.streamthoughts.azkarra.api.config.Configurable} interface.
     * @param configuration the {@link Conf}.
     * @return              a new {@link ComponentDescriptorModifier} instance.
     */
    public static ComponentDescriptorModifier withConfig(final Conf configuration) {
        return new ComponentDescriptorModifier() {
            @Override
            public <T> ComponentDescriptor<T> apply(final ComponentDescriptor<T> descriptor) {
                return ComponentDescriptorBuilder.<T>create(descriptor)
                    .configuration(configuration)
                    .build();
            }
        };
    }
}

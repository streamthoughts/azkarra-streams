/*
 * Copyright 2019-2021 StreamThoughts.
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
import io.streamthoughts.azkarra.api.components.GettableComponent;
import io.streamthoughts.azkarra.api.components.Qualifier;
import io.streamthoughts.azkarra.api.components.Restriction;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.runtime.components.condition.ConfigConditionalContext;

import java.util.Optional;

public class RestrictedComponentFactory {

    private final ComponentFactory factory;

    public RestrictedComponentFactory(final ComponentFactory factory) {
        this.factory = factory;
    }

    /**
     * Finds a component for the given type that is available for the given config and restriction.
     *
     * @param componentType        the {@link Class } of the component.
     * @param componentConfig      the {@link Conf} object to be used for resolved available components.
     * @param restriction          the {@link Restriction}.
     * @param <T>                  the component type.
     * @return                     an optional {@link GettableComponent}.
     */
    public <T> Optional<GettableComponent<T>> findComponentByRestriction(final Class<T> componentType,
                                                                          final Conf componentConfig,
                                                                          final Restriction restriction) {

        final Qualifier<T> qualifier = Qualifiers.byRestriction(restriction);
        if (factory.containsComponent(componentType, qualifier)) {
            GettableComponent<T> provider = factory.getComponentProvider(componentType, qualifier);
            if (provider.isEnable(new ConfigConditionalContext<>(componentConfig)))
                return Optional.of(provider);
        }
        return Optional.empty();
    }
}

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
package io.streamthoughts.azkarra.api.components.condition;

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.config.Conf;

/**
 * The context information for use by a {@link Condition} matching a {@link ComponentDescriptor}.
 * 
 * @see Condition#matches(ConditionContext).
 */
public interface ConditionContext<T extends ComponentDescriptor> {

    /**
     * Get the {@link ComponentFactory} that holds the component descriptor should the condition match.
     * The returned registry should not be used to register a new component descriptor.
     *
     * @return  the {@link ComponentFactory}; cannot be {@code null}.
     */
    ComponentFactory getComponentFactory();

    /**
     * Get the configuration that was passed to get the component instance.
     *
     * @return  the {@link Conf} object; cannot be {@code null}.
     */
    Conf getConfig();

    /**
     * @return the component that should match the condition.
     */
    T getComponent();

}

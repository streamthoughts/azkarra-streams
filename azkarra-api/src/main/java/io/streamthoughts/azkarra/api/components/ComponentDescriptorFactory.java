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
package io.streamthoughts.azkarra.api.components;

import java.util.function.Supplier;

/**
 * Factory to create new {@link SimpleComponentDescriptor} instance.
 */
public interface ComponentDescriptorFactory {

    /**
     * Makes a new {@link SimpleComponentDescriptor} instance.
     *
     * @param componentName      the name of the component (can be {@code null}.
     * @param componentType      the type of the component.
     * @param componentSupplier  the supplier of the component.
     * @param isSingleton        is the component singleton.
     *
     * @return                   a new instance of {@link ComponentDescriptor}.
     *
     * @throws ComponentRegistrationException if an exception occurred while building the descriptor.
     */
    <T> ComponentDescriptor<T> make(final String componentName,
                                    final Class<T> componentType,
                                    final Supplier<T> componentSupplier,
                                    final boolean isSingleton);
}
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

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorModifier;

public class ComponentDescriptorModifiers {

    public static ComponentDescriptorModifier withOrder(final int order) {
        return new ComponentDescriptorModifier() {
            @Override
            public <T> ComponentDescriptor<T> apply(final ComponentDescriptor<T> descriptor) {
                return ComponentDescriptorBuilder.<T>create()
                    .name(descriptor.name())
                    .version(descriptor.version().toString())
                    .type(descriptor.type())
                    .metadata(descriptor.metadata())
                    .classLoader(descriptor.classLoader())
                    .supplier(descriptor.supplier())
                    .isSingleton(descriptor.isSingleton())
                    .order(order)
                    .build();
            }
        };
    }
}

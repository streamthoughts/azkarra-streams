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
import io.streamthoughts.azkarra.api.components.condition.Conditions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComponentDescriptorBuilderTest {

    @Test
    public void shouldCreateDescriptorGivenEmptyBuilder() {
        final ComponentDescriptor<ComponentDescriptorBuilderTest> descriptor
                = ComponentDescriptorBuilder.<ComponentDescriptorBuilderTest>create()
            .type(ComponentDescriptorBuilderTest.class)
            .supplier(ComponentDescriptorBuilderTest::new)
            .isSingleton(true)
            .build();
        assertNotNull(descriptor);
        assertTrue(descriptor.isSingleton());
    }

    @Test
    public void shouldCreateDescriptorFromAnotherOne() {
        final ComponentDescriptor<ComponentDescriptorBuilderTest> base
                = ComponentDescriptorBuilder.<ComponentDescriptorBuilderTest>create()
            .type(ComponentDescriptorBuilderTest.class)
            .supplier(ComponentDescriptorBuilderTest::new)
            .isSingleton(true)
            .isEager(true)
            .isPrimary(true)
            .isSecondary(true)
            .condition(Conditions.onPropertyTrue("???"))
            .version("1.0")
            .build();
        final ComponentDescriptor<ComponentDescriptorBuilderTest> created = ComponentDescriptorBuilder.create(base).build();
        assertEquals(created.toString(), base.toString());
    }

}
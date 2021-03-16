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
package io.streamthoughts.azkarra.api.components;

import io.streamthoughts.azkarra.api.ApplicationId;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.ApplicationIdBuilder;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComponentNameGeneratorTest {

    @Test
    public void testDefaultImplementation() {

        ComponentDescriptor<ComponentNameGeneratorTest> descriptor = new SimpleComponentDescriptor<>(
            "NAME",
            ComponentNameGeneratorTest.class,
            ComponentNameGeneratorTest::new,
            true);
        String name = ComponentNameGenerator.DEFAULT.generate(descriptor);
        assertNotNull(name);
        assertEquals("componentNameGeneratorTest", name);
    }

    @Test
    public void testGivenAnonymousClass() {
        ComponentDescriptor descriptor = new SimpleComponentDescriptor(
            "NAME",
            new Object(){}.getClass(),
            Object::new,
            true
        );

        String name = ComponentNameGenerator.DEFAULT.generate(descriptor);
        assertNotNull(name);
        assertEquals("componentNameGeneratorTest$1", name);
    }
}
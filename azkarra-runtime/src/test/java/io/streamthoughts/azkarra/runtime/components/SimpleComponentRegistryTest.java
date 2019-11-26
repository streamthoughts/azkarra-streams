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
import io.streamthoughts.azkarra.api.components.NoSuchComponentException;
import io.streamthoughts.azkarra.api.components.NoUniqueComponentException;
import io.streamthoughts.azkarra.api.config.Conf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleComponentRegistryTest {

    private DefaultComponentRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = new DefaultComponentRegistry();
        registry.setComponentAliasesGenerator(new ClassComponentAliasesGenerator());
    }

    @Test
    public void shouldThrowNoSuchComponentExceptionGivenInvalidType() {
        final NoSuchComponentException e = assertThrows(
            NoSuchComponentException.class,
            () -> registry.getComponent(TestB.class, Conf.empty()));
        assertEquals(
            "No component registered for class '" + TestB.class.getName() + "'",
            e.getMessage());
    }

    @Test
    public void shouldThrowNoUniqueComponentExceptionGivenInvalidType() {
        registry.registerComponent(new ComponentDescriptor<>(TestB.class));
        registry.registerComponent(new ComponentDescriptor<>(TestB.class));
        NoUniqueComponentException e = assertThrows(
            NoUniqueComponentException.class,
            () -> registry.getComponent(TestB.class, Conf.empty()));
        assertEquals(
            "Expected single matching component for class '" + TestB.class.getName() + "' but found 2",
            e.getMessage());
    }

    @Test
    public void shouldGetLatestComponentVersion() {
        registry.registerComponent(new ComponentDescriptor<>(TestB.class, "1"));
        registry.registerComponent(new ComponentDescriptor<>(TestB.class, "2.1"));
        registry.registerComponent(new ComponentDescriptor<>(TestC.class, "2.3-SNAPSHOT"));

        TestB component = registry.getLatestComponent(TestB.class, Conf.empty());
        assertNotNull(component);
        assertTrue(TestC.class.isAssignableFrom(component.getClass()));
    }

    @Test
    public void shouldGetEmptyListWhenSearchingForDescriptorGivenInvalidType() {
        assertTrue(registry.findDescriptorByAlias("invalid").isEmpty());
    }

    @Test
    public void shouldGetNewComponentInstanceGivenValidType() {
        registry.registerComponent(new ComponentDescriptor<>(TestB.class));
        TestA component = registry.getComponent(TestB.class, Conf.empty());
        assertNotNull(component);
    }

    @Test
    public void shouldGetComponentForAllSubTypes() {
        registry.registerComponent(new ComponentDescriptor<>(TestC.class));

        assertNotNull(registry.getComponent(TestC.class, Conf.empty()));
        assertNotNull(registry.getComponent(TestB.class, Conf.empty()));
        assertNotNull(registry.getComponent(TestA.class, Conf.empty()));
    }

    @Test
    public void shouldGetAllComponentsGivenValidType() {
        registry.registerComponent(new ComponentDescriptor<>(TestC.class));
        registry.registerComponent(new ComponentDescriptor<>(TestB.class));
        Collection<TestB> components = registry.getAllComponents(TestB.class, Conf.empty());
        assertEquals(2, components.size());
    }

    @Test
    public void shouldGetAllComponentsGivenValidAlias() {
        registry.registerComponent(new ComponentDescriptor<>(TestB.class));
        TestB component = registry.getComponent("TestB", Conf.empty());
        assertNotNull(component);
    }

    interface TestA {

    }

    static class TestB implements TestA {

    }

    static class TestC extends TestB {

    }
}
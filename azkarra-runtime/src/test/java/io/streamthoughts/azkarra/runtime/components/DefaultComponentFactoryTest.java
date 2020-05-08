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

import io.streamthoughts.azkarra.api.annotations.Order;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ConflictingComponentDefinitionException;
import io.streamthoughts.azkarra.api.components.NoSuchComponentException;
import io.streamthoughts.azkarra.api.components.NoUniqueComponentException;
import io.streamthoughts.azkarra.api.components.SimpleComponentDescriptor;
import io.streamthoughts.azkarra.api.components.condition.Conditions;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import io.streamthoughts.azkarra.api.util.Version;
import io.streamthoughts.azkarra.runtime.components.condition.ConfigConditionalContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultComponentFactoryTest {

    private static final String COMPONENT_NAME = "NAME";

    private DefaultComponentFactory factory;

    @BeforeEach
    public void setUp() {
        factory = new DefaultComponentFactory(new DefaultComponentDescriptorFactory());
        factory.setComponentAliasesGenerator(new ClassComponentAliasesGenerator());
    }

    @Test
    public void shouldThrowNoSuchComponentExceptionGivenInvalidType() {
        final NoSuchComponentException e = assertThrows(
            NoSuchComponentException.class,
            () -> factory.getComponent(TestB.class, Conf.empty()));
        assertEquals(
            "No component registered for type '" + TestB.class.getName() + "'",
            e.getMessage());
    }

    @Test
    public void shouldThrowNoUniqueComponentExceptionGivenTwoSameTypeComponents() {
        factory.registerComponent("componentOne", TestB.class);
        factory.registerComponent("componentTwo", TestB.class);
        NoUniqueComponentException e = assertThrows(
            NoUniqueComponentException.class,
            () -> factory.getComponent(TestB.class, Conf.empty()));
        assertEquals(
            "Expected single matching component for type '" + TestB.class.getName() + "' but found 2",
            e.getMessage());
    }

    @Test
    public void shouldNotThrowNoUniqueComponentExceptionGivenTwoSameTypeComponentsUsingPrimary() {
        factory.registerComponent("componentOne", TestB.class, TestB::new);
        factory.registerComponent("componentTwo", TestB.class, TestB::new, asPrimary());

        Optional<ComponentDescriptor<TestB>> descriptor = factory.findDescriptorByClass(TestB.class);
        Assertions.assertTrue(descriptor.isPresent());
        Assertions.assertEquals("componentTwo", descriptor.get().name());
    }

    @Test
    public void shouldNotThrowNoUniqueComponentExceptionGivenTwoSameTypeComponentsUsingSecondary() {
        factory.registerComponent("componentOne", TestB.class, TestB::new);
        factory.registerComponent("componentTwo", TestB.class, TestB::new, asSecondary());

        Optional<ComponentDescriptor<TestB>> descriptor = factory.findDescriptorByClass(TestB.class);
        Assertions.assertTrue(descriptor.isPresent());
        Assertions.assertEquals("componentOne", descriptor.get().name());
    }


    @Test
    public void shouldThrowConflictingComponentExceptionGivenTwoIdenticalComponents() {
        factory.registerComponent(TestB.class);
        ConflictingComponentDefinitionException e = assertThrows(ConflictingComponentDefinitionException.class,
            () -> factory.registerComponent(TestB.class));
        assertEquals("Failed to resister ComponentDescriptor, component already exists for key: [type="
                        + TestB.class.getName() + ", qualifier=@Named(testB)]",
            e.getMessage());
    }

    @Test
    public void shouldReturnEmptyDescriptorListGivenInvalidAlias() {
        assertTrue(factory.findDescriptorByAlias("invalid").isEmpty());
    }

    @Test
    public void shouldReturnNewInstanceForPrototypeComponent() {
        factory.registerDescriptor(new SimpleComponentDescriptor<>(COMPONENT_NAME, TestB.class, TestB::new, false));
        TestA object1 = factory.getComponent(TestB.class, Conf.empty());
        TestA object2 = factory.getComponent(TestB.class, Conf.empty());
        assertNotNull(object1);
        assertNotNull(object2);
        assertNotEquals(object1, object2);
    }

    @Test
    public void shouldNotReturnNewInstanceForSingletonComponent() {
        factory.registerDescriptor(new SimpleComponentDescriptor<>(COMPONENT_NAME, TestB.class, TestB::new, true));
        TestA object1 = factory.getComponent(TestB.class, Conf.empty());
        TestA object2 = factory.getComponent(TestB.class, Conf.empty());
        assertNotNull(object1);
        assertNotNull(object2);
        assertEquals(object1, object2);
    }

    @Test
    public void shouldFindComponentDescriptorForAllSubTypes() {
        factory.registerDescriptor(new SimpleComponentDescriptor<>(COMPONENT_NAME, TestC.class, TestC::new, false));
        assertTrue(factory.findDescriptorByClass(TestC.class).isPresent());
        assertTrue(factory.findDescriptorByClass(TestB.class).isPresent());
        assertTrue(factory.findDescriptorByClass(TestA.class).isPresent());
    }

    @Test
    public void shouldFindComponentDescriptorGivenQualifierByVersion() {
        factory.registerDescriptor(new SimpleComponentDescriptor<>(COMPONENT_NAME, TestC.class, TestC::new, "2.1", false));
        factory.registerDescriptor(new SimpleComponentDescriptor<>(COMPONENT_NAME, TestC.class, TestC::new, "1.1", false));

        Optional<ComponentDescriptor<TestA>> descriptor =
                factory.findDescriptorByClass(TestA.class, Qualifiers.byVersion("1.1"));

        assertTrue(descriptor.isPresent());
        assertTrue(Version.isEqual(descriptor.get().version(), "1.1"));
    }

    @Test
    public void shouldFindComponentDescriptorGivenQualifierByLatestVersion() {
        factory.registerDescriptor(new SimpleComponentDescriptor<>(
                COMPONENT_NAME, TestB.class, TestB::new,"1", false));
        factory.registerDescriptor(new SimpleComponentDescriptor<>(
                COMPONENT_NAME, TestB.class, TestB::new,"2.1", false));
        factory.registerDescriptor(new SimpleComponentDescriptor<>(
                COMPONENT_NAME, TestC.class, TestC::new, "2.3-SNAPSHOT", false));

        Optional<ComponentDescriptor<TestB>> descriptor =
                factory.findDescriptorByClass(TestB.class, Qualifiers.byLatestVersion());

        assertTrue(descriptor.isPresent());
        assertTrue(Version.isEqual(descriptor.get().version(), "2.3-SNAPSHOT"));
    }

    @Test
    public void shouldFindAllComponentDescriptorGivenASubType() {
        factory.registerDescriptor(new SimpleComponentDescriptor<>(COMPONENT_NAME, TestC.class, TestC::new, false));
        factory.registerDescriptor(new SimpleComponentDescriptor<>(COMPONENT_NAME, TestB.class, TestB::new, false));
        Collection<ComponentDescriptor<TestB>> descriptors = factory.findAllDescriptorsByClass(TestB.class);
        assertEquals(2, descriptors.size());
    }

    @Test
    public void shouldFindComponentDescriptorGivenAlias() {
        factory.registerDescriptor(new SimpleComponentDescriptor<>(COMPONENT_NAME, TestB.class, TestB::new, false));
        Optional<ComponentDescriptor<Object>> descriptor = factory.findDescriptorByAlias("TestB");
        assertTrue(descriptor.isPresent());
    }

    @Test
    public void shouldNotReturnConditionalComponentGivenNotMatchingContext() {
        factory.registerComponent("condComponent", TestB.class, TestB::new, withConditions(Conditions.onPropertyExist("component.enable")));
        Assertions.assertTrue(factory.findDescriptorByClass(TestB.class).isPresent());
        Assertions.assertTrue(factory.findDescriptorByClass(TestB.class, new ConfigConditionalContext(Conf.empty())).isEmpty());
    }

    @Test
    public void shouldGetAllComponentSortedByOrder() {
        factory.registerSingleton(TestC.class, TestC::new);
        factory.registerSingleton(TestB.class, TestB::new);
        factory.registerSingleton(TestD.class, TestD::new);

        List<TestA> components = (List<TestA>)factory.getAllComponents(TestA.class, Conf.empty());

        assertEquals(1, components.get(0).value());
        assertEquals(2, components.get(1).value());
        assertEquals(3, components.get(2).value());
    }

    @Test
    public void shouldInitializedEagerComponentOnInit() {
        var supplier = new SpySupplier<>(TestB::new);
        factory.registerSingleton(TestB.class, supplier, asEager());
        Assertions.assertFalse(supplier.initialized);
        factory.init(Conf.empty());
        Assertions.assertTrue(supplier.initialized);
    }

    private static class SpySupplier<T extends TestA> implements Supplier<T> {

        boolean initialized = false;

        final Supplier<T> supplier;

        private SpySupplier(final Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            initialized = true;
            return supplier.get();
        }
    }

    interface TestA {
        default int value() { return 0; }
    }

    @Order(1)
    static class TestB implements TestA {
        public int value() { return 1; }
    }

    @Order(2)
    static class TestC extends TestB {
        public int value() { return 2; }
    }

    @Order(3)
    static class TestD implements TestA {
        public int value() { return 3; }
    }
}
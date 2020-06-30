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

import io.streamthoughts.azkarra.api.annotations.ConfValue;
import io.streamthoughts.azkarra.api.annotations.Order;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorFactory;
import io.streamthoughts.azkarra.api.components.Versioned;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.util.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultComponentDescriptorFactoryTest {

    private ComponentDescriptorFactory factory = new DefaultComponentDescriptorFactory();

    @Test
    public void shouldCreateDescriptorGivenSimpleClass() {
        ComponentDescriptor<TestClass> descriptor = factory.make(
            null, TestClass.class, TestClass::new, true);

        assertNotNull(descriptor);
        assertFalse(descriptor.isVersioned());
    }

    @Test
    public void shouldCreateDescriptorAndLoadConfigFromAnnotationMetadata() {
        ComponentDescriptor<TestClassWithAnnotations> descriptor = factory.make(
            null, TestClassWithAnnotations.class, TestClassWithAnnotations::new, true);
        assertNotNull(descriptor);
        assertTrue(descriptor.metadata().contains("confvalue", "value", "value1"));
        assertTrue(descriptor.metadata().contains("confvalue", "value", "value2"));
        Conf configuration = descriptor.configuration();
        assertNotNull(configuration);
        assertTrue(configuration.hasPath("key-1"));
        assertTrue(configuration.hasPath("key-2"));
    }

    @Test
    public void shouldCreateDescriptorAndLoadOrderFromAnnotationMetadata() {
        ComponentDescriptor<TestClassWithAnnotations> descriptor = factory.make(
                null, TestClassWithAnnotations.class, TestClassWithAnnotations::new, true);
        assertNotNull(descriptor);
        assertTrue(descriptor.metadata().contains("order", "value", 42));
        assertEquals(42, descriptor.order());
    }

    @Test
    public void shouldCreateDescriptorGivenVersionedClass() {
        ComponentDescriptor<TestVersionedClass> descriptor = factory.make(
            null, TestVersionedClass.class, TestVersionedClass::new, true);
        assertNotNull(descriptor);
        assertTrue(descriptor.isVersioned());
        assertTrue(Version.isEqual(descriptor.version(), "1.0"));
    }

    private static class TestClass {

    }

    @Order(42)
    @ConfValue(key = "key-1", value = "value1")
    @ConfValue(key = "key-2", value = "value2")
    private static class TestClassWithAnnotations {

    }

    public static class TestVersionedClass implements Versioned {

        @Override
        public String version() {
            return "1.0";
        }
    }
}
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

import io.streamthoughts.azkarra.api.annotations.DefaultStreamsConfig;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorFactory;
import io.streamthoughts.azkarra.api.components.Versioned;
import io.streamthoughts.azkarra.api.util.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void shouldCreateDescriptorGivenVersionedClass() {
        ComponentDescriptor<TestClassWithAnnotations> descriptor = factory.make(
            null, TestClassWithAnnotations.class, TestClassWithAnnotations::new, true);
        assertNotNull(descriptor);
        assertFalse(descriptor.isVersioned());

        descriptor.metadata().contains("DefaultStreamsConfig", "key-1", "value1");
        descriptor.metadata().contains("DefaultStreamsConfig", "key-2", "value2");
    }

    @Test
    public void shouldCreateDescriptorGivenAnnotatedClass() {
        ComponentDescriptor<TestVersionedClass> descriptor = factory.make(
            null, TestVersionedClass.class, TestVersionedClass::new, true);
        assertNotNull(descriptor);
        assertTrue(Version.isEqual(descriptor.version(), "1.0"));
    }

    private static class TestClass {

    }

    @DefaultStreamsConfig(name = "key-1", value = "value1")
    @DefaultStreamsConfig(name = "key-2", value = "value2")
    private static class TestClassWithAnnotations {

    }

    public static class TestVersionedClass implements Versioned {

        @Override
        public String version() {
            return "1.0";
        }
    }
}
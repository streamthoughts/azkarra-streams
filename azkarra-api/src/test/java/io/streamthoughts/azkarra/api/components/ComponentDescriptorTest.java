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

import io.streamthoughts.azkarra.api.util.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComponentDescriptorTest {

    @Test
    public void shouldReturnTrueIfComponentIsCloseable() {
        ComponentDescriptor<CloseableComponent> component = newComponent(null);
        Assertions.assertTrue(component.isCloseable());
    }

    @Test
    public void shouldReturnFalseIfComponentIsNotVersioned() {
        ComponentDescriptor<CloseableComponent> component = newComponent(null);
        Assertions.assertFalse(component.isVersioned());
    }

    @Test
    public void shouldReturnTrueIfComponentIsNotVersioned() {
        ComponentDescriptor<CloseableComponent> component = newComponent("1.0");
        Assertions.assertTrue(component.isVersioned());
    }

    @Test
    public void shouldCompareVersionedComponents() {
        Optional<ComponentDescriptor<CloseableComponent>> latest = Stream.of("1", "2")
            .map(this::newComponent)
            .sorted(ComponentDescriptor.ORDER_BY_VERSION)
            .findFirst();
        assertTrue(Version.isEqual(latest.get().version(), "2"));
    }

    @Test
    public void shouldCompareOrderedComponents() {
        List<ComponentDescriptor<CloseableComponent>> list = Stream.of(3, 2, 1)
                .map(this::newComponent)
                .sorted(ComponentDescriptor.ORDER_BY_ORDER)
                .collect(Collectors.toList());
        assertEquals(1, list.get(0).order());
        assertEquals(2, list.get(1).order());
        assertEquals(3, list.get(2).order());
    }

    @Test
    public void shouldCompareComponentGivenNotVersioned() {
        ComponentDescriptor<CloseableComponent> c1 = newComponent(null);
        ComponentDescriptor<CloseableComponent> c2 = newComponent("1.0");
        Optional<ComponentDescriptor<CloseableComponent>> latest = Stream.of(c1, c2)
                .sorted(ComponentDescriptor.ORDER_BY_VERSION).findFirst();
        assertTrue(Version.isEqual(latest.get().version(), "1.0"));
    }

    private ComponentDescriptor<CloseableComponent> newComponent(final int order) {
        return new SimpleComponentDescriptor<>(
                "name",
                CloseableComponent.class,
                CloseableComponent.class.getClassLoader(),
                () -> null,
                null,
                true,
                false,
                false,
                order
        );
    }

    private ComponentDescriptor<CloseableComponent> newComponent(final String version) {
        return new SimpleComponentDescriptor<>(
            "name",
            CloseableComponent.class,
            CloseableComponent.class.getClassLoader(),
            () -> null,
            version,
            true,
            false,
            false,
            Ordered.LOWEST_ORDER
        );
    }

    static class CloseableComponent implements Closeable {

        @Override
        public void close() {

        }
    }

}
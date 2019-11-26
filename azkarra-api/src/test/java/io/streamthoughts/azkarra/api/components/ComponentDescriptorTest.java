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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.Optional;
import java.util.stream.Stream;

public class ComponentDescriptorTest {

    @Test
    public void shouldReturnTrueIfComponentIsCloseable() {
        Assertions.assertTrue(new ComponentDescriptor<>(CloseableComponent.class).isCloseable());
    }

    @Test
    public void shouldReturnFalseIfComponentIsNotVersioned() {
        Assertions.assertFalse(new ComponentDescriptor<>(CloseableComponent.class).isVersioned());
    }

    @Test
    public void shouldReturnTrueIfComponentIsNotVersioned() {
        Assertions.assertTrue(new ComponentDescriptor<>(CloseableComponent.class, "1").isVersioned());
    }

    @Test
    public void shouldCompareVersionedComponent() {
        Optional<ComponentDescriptor<CloseableComponent>> latest = Stream.of("1", "2")
            .map(v -> new ComponentDescriptor<>(CloseableComponent.class, v))
            .sorted()
            .findFirst();
        Assertions.assertEquals("2", latest.get().version());
    }

    @Test
    public void shouldCompareComponentGivenNotVersioned() {
        ComponentDescriptor<CloseableComponent> c1 = new ComponentDescriptor<>(CloseableComponent.class);
        ComponentDescriptor<CloseableComponent> c2 = new ComponentDescriptor<>(CloseableComponent.class, "1");
        Optional<ComponentDescriptor<CloseableComponent>> latest = Stream.of(c1, c2).sorted().findFirst();

        Assertions.assertEquals("1", latest.get().version());
    }

    static class CloseableComponent implements Closeable {

        @Override
        public void close() {

        }
    }

}
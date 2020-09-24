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
package io.streamthoughts.azkarra.api.components.qualifier;

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.SimpleComponentDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnyQualifierTest {

    @Test
    public void shouldReturnAnyComponentMatchingOneRestriction() {
        final var descriptorA = new SimpleComponentDescriptor<>(
                "A", Object.class, Object::new, true);

        final var descriptorB = new SimpleComponentDescriptor<>(
                "B", Object.class, Object::new, true);

        final var descriptorC = new SimpleComponentDescriptor<>(
                "C", Object.class, Object::new, true);

        final var qualifier = new AnyQualifier<>(
            Arrays.asList(Qualifiers.byName("A"), Qualifiers.byName("C"))
        );
        List<String> filtered = qualifier
                .filter(Object.class, Stream.of(descriptorA, descriptorB, descriptorC))
                .map(ComponentDescriptor::name)
                .collect(Collectors.toList());
        Assertions.assertEquals(2, filtered.size());
        Assertions.assertTrue(filtered.containsAll(Arrays.asList("A", "C")));
    }
}
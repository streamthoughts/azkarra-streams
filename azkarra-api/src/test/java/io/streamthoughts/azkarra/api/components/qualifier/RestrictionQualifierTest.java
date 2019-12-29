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
package io.streamthoughts.azkarra.api.components.qualifier;

import io.streamthoughts.azkarra.api.components.ComponentAttribute;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.Restriction;
import io.streamthoughts.azkarra.api.components.SimpleComponentDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RestrictionQualifierTest {

    private ComponentDescriptor<Object> descriptor;

    @BeforeEach
    public void setUp() {
        descriptor = new SimpleComponentDescriptor<>(
                "name", Object.class, Object::new, true);
    }

    @Test
    public void shouldAcceptGivenComponentDescriptorWithNoRestrictionWhenRestrictionIsApplication() {
        RestrictionQualifier<Object> qualifier = new RestrictionQualifier<>(Restriction.application());

        List<ComponentDescriptor<Object>> filtered = qualifier.filter(Object.class, Stream.of(descriptor))
                .collect(Collectors.toList());
        assertEquals(1, filtered.size());
    }

    @Test
    public void shouldNotAcceptGivenNonMatchingComponentDescriptor() {
        RestrictionQualifier<Object> qualifier = new RestrictionQualifier<>(Restriction.streams("test"));

        List<ComponentDescriptor<Object>> filtered = qualifier.filter(Object.class, Stream.of(descriptor))
                .collect(Collectors.toList());
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void shouldNotAcceptGivenRestrictionMatchingComponentDescriptor() {
        RestrictionQualifier<Object> qualifier = new RestrictionQualifier<>(Restriction.streams("test"));

        ComponentAttribute restriction = new ComponentAttribute("restricted");
        restriction.add("type", Restriction.TYPE_STREAMS);
        restriction.add("names", new String[]{"test"});
        descriptor.metadata().addAttribute(restriction);
        List<ComponentDescriptor<Object>> filtered = qualifier.filter(Object.class, Stream.of(descriptor))
                .collect(Collectors.toList());
        assertEquals(1, filtered.size());
    }
}
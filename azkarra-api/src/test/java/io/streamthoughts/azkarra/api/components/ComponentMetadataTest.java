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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComponentMetadataTest {

    private ComponentMetadata metadata;

    @BeforeEach
    public void setUp() {
        metadata = new ComponentMetadata();
        ComponentAttribute testAttribute1 = new ComponentAttribute("testAttribute");
        testAttribute1.add("simpleMember", "valueOne", "default");
        testAttribute1.add("arrayMember", new String[]{"one", "two"}, "default");
        metadata.addAttribute(testAttribute1);

        ComponentAttribute testAttribute2 = new ComponentAttribute("testAttribute");
        testAttribute2.add("simpleMember", "valueTwo", "default");
        metadata.addAttribute(testAttribute2);

        ComponentAttribute testAttribute3 = new ComponentAttribute("customAttribute");
        testAttribute3.add("simpleMember", "valueTwo", "default");
        metadata.addAttribute(testAttribute3);
    }

    @Test
    public void shouldGetAllAttributes() {
        assertEquals(3, metadata.attributes().size());
    }

    @Test
    public void shouldGetAllMemberValue() {
        assertEquals(2, metadata.attributesForName("testAttribute").size());
    }

    @Test
    public void shouldGetAttributeValueMember() {
        Optional<Object> value = metadata.value("testAttribute", "simpleMember");
        assertTrue(value.isPresent());
        assertEquals("valueOne", value.get());
    }

    @Test
    public void shouldGetAllAttributeValueMember() {
        Collection<String> values = metadata.stringValues("testAttribute", "simpleMember");
        assertEquals(2, values.size());
        assertTrue(values.contains("valueOne"));
        assertTrue(values.contains("valueTwo"));
    }
}
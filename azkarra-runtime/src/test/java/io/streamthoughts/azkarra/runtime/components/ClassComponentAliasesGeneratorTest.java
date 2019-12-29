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
import io.streamthoughts.azkarra.api.components.SimpleComponentDescriptor;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClassComponentAliasesGeneratorTest {

    private final ComponentDescriptor<TestTopologyProvider> descriptor = new SimpleComponentDescriptor<>(
        "DUMMY",
        TestTopologyProvider.class,
        TestTopologyProvider::new,
        false
    );

    @Test
    public void shouldGenerateAliasesForTopologyProvider() {

        ClassComponentAliasesGenerator generator = new ClassComponentAliasesGenerator();
        Set<String> aliases = generator.getAliasesFor(descriptor, Collections.emptyList());
        assertTrue(aliases.contains("TestTopologyProvider"));
        assertTrue(aliases.contains("TestTopology"));
        assertTrue(aliases.contains("Test"));
    }

    static class TestTopologyProvider implements TopologyProvider {

        @Override
        public String version() {
            return null;
        }

        @Override
        public Topology get() {
            return null;
        }
    }

}
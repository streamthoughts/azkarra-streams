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
package io.streamthoughts.azkarra.runtime.components;

import io.streamthoughts.azkarra.api.annotations.ConfValue;
import io.streamthoughts.azkarra.api.annotations.TopologyInfo;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorFactory;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopologyDescriptorFactoryTest {

    private ComponentDescriptorFactory factory = new DefaultComponentDescriptorFactory();

    @Test
    public void test() {

        ComponentDescriptor<TestTopologyProvider> descriptor = factory.make(
            null,
            TestTopologyProvider.class,
            TestTopologyProvider::new,
            true
        );

        var topologyDescriptor = new TopologyDescriptor<>(descriptor);

        assertEquals(TestTopologyProvider.class.getName(), topologyDescriptor.className());
        assertEquals("CustomAlias", topologyDescriptor.aliases().iterator().next());
        assertEquals("1.0", topologyDescriptor.version().toString());
        assertEquals("test description", topologyDescriptor.description());
        assertEquals("2", topologyDescriptor.configuration().getString(StreamsConfig.NUM_STREAM_THREADS_CONFIG));
        assertEquals("static.app.id", topologyDescriptor.configuration().getString(StreamsConfig.APPLICATION_ID_CONFIG));
    }

    @TopologyInfo( description = "test description", aliases = "CustomAlias")
    @ConfValue(key = StreamsConfig.NUM_STREAM_THREADS_CONFIG, value = "2")
    @ConfValue(key = StreamsConfig.APPLICATION_ID_CONFIG, value = "static.app.id")
    public static class TestTopologyProvider implements TopologyProvider {

        @Override
        public String version() {
            return "1.0";
        }

        @Override
        public Topology topology() {
            return null;
        }
    }
}
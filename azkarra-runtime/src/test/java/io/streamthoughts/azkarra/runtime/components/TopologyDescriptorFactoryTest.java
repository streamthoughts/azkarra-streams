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
import io.streamthoughts.azkarra.api.annotations.TopologyInfo;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TopologyDescriptorFactoryTest {

    @Test
    public void should() {
        final TopologyDescriptorFactory factory = new TopologyDescriptorFactory();
        ComponentDescriptor<TopologyProvider> descriptor = factory.make(TestTopologyProvider.class, "1.0");

        assertTrue(descriptor.isVersioned());
        assertEquals("1.0", descriptor.version());
        assertEquals(TestTopologyProvider.class.getName(), descriptor.className());
        assertTrue(descriptor.aliases().contains("CustomAlias"));
        assertTrue((descriptor instanceof TopologyDescriptor));
        TopologyDescriptor topologyDescriptor = (TopologyDescriptor) descriptor;
        assertEquals("test description", topologyDescriptor.description());
        Conf conf = topologyDescriptor.streamsConfigs();
        assertEquals("2", conf.getString(StreamsConfig.NUM_STREAM_THREADS_CONFIG));
    }

    @TopologyInfo( description = "test description", aliases = "CustomAlias")
    @DefaultStreamsConfig(name = StreamsConfig.NUM_STREAM_THREADS_CONFIG, value = "2")
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
/*
 * Copyright 2019 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy with the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.azkarra.runtime.streams.topology;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.runtime.MockTopologyProvider;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.topology.TopologyContainer;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyDescription;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TopologyFactoryTest {

    private static Topology MOCK = Mockito.mock(Topology.class);

    static {
        Mockito.when(MOCK.describe()).thenReturn(Mockito.mock(TopologyDescription.class));
    }

    private final TopologyFactory factory = new TopologyFactory(DefaultStreamsExecutionEnvironment.create());

    @Test
    public void shouldCreateTopologyContainer() {
        TopologyContainer container = factory.make(
            new DummyTopologyProvider(),
            Conf.empty(),
            Executed.as("dummy-topology").withDescription("user-description"));

        TopologyMetadata metadata = container.getMetadata();
        assertNotNull(metadata);
        assertEquals("dummy-topology", metadata.name());
        assertEquals("1.0", metadata.version());
        assertEquals("user-description", metadata.description());
        assertNotNull(metadata.topology());

        Topology topology = container.getTopology();
        assertEquals(MOCK, topology);
    }

    @Test
    public void shouldCreateConfigurableTopologyContainer() {
        Executed executed = Executed.as("dummy-topology")
            .withDescription("user-description")
            .withConfig(Conf.with("version", "configured-version"));

        TopologyContainer container = factory.make(
            new ConfigurableTopologyProvider(),
            Conf.empty(),
            executed);

        TopologyMetadata metadata = container.getMetadata();
        assertNotNull(metadata);
        assertEquals("configured-version", metadata.version());
    }

    public static class DummyTopologyProvider extends MockTopologyProvider {

        DummyTopologyProvider() {
            super("1.0", MOCK);
        }
    }

    public static class ConfigurableTopologyProvider extends MockTopologyProvider implements Configurable {

        ConfigurableTopologyProvider() {
            super(null, MOCK);
        }

        @Override
        public void configure(Conf configuration) {
            version = configuration.getString("version");
        }
    }

}
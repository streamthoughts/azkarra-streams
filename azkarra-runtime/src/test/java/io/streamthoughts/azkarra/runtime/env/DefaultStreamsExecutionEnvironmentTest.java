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
package io.streamthoughts.azkarra.runtime.env;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.streams.topology.TopologyDefinition;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyDescription;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultStreamsExecutionEnvironmentTest {

    private static Topology MOCK = Mockito.mock(Topology.class);

    static {
        Mockito.when(MOCK.describe()).thenReturn(Mockito.mock(TopologyDescription.class));
    }

    @Test
    public void shouldFallbackToEnvCongWhenInitializingTopologies() {

        DefaultStreamsExecutionEnvironment environment = (DefaultStreamsExecutionEnvironment)
                DefaultStreamsExecutionEnvironment.create();

        var envConf = Conf.of(
            "streams.prop", "value",
            "streams.default.prop", "value",
            "prop", "value",
            "default.prop", "value"
        );

        environment.setConfiguration(envConf);

        Executed executed = Executed.as("dummy-topology", "a test topology")
            .withConfig(Conf.of(
                "streams.user.prop", "value",
                "streams.default.prop", "override",
                "user.prop", "value",
                "default.prop", "override"
            ));

        var holder = environment.new TopologyDefinitionHolder(
            DummyTopologyProvider::new,
            executed);

        var definition = holder.createTopologyDefinition();
        assertEquals(MOCK, definition.getTopology());

        assertEquals("dummy-topology",  definition.getName());
        assertEquals("a test topology",  definition.getDescription());

        Conf streams = holder.getTopologyConfig().getSubConf("streams");
        assertEquals("value", streams.getString("prop") );
        assertEquals("value", streams.getString("user.prop") );
        assertEquals("override", streams.getString("default.prop") );
    }

    public static class DummyTopologyProvider implements TopologyProvider, Configurable {

        public Conf configuration;

        @Override
        public void configure(final Conf configuration) {
            this.configuration = configuration;
        }

        @Override
        public String version() {
            return "1.0";
        }

        @Override
        public Topology get() {
            return MOCK;
        }
    }

}
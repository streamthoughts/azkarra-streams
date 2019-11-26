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
package io.streamthoughts.azkarra.streams.context.internal;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.ConfBuilder;
import io.streamthoughts.azkarra.api.errors.MissingConfException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TopologyConfigTest {

    @Test
    public void shouldCreateTopologyConfigGivenValidConf() {

        Conf conf = ConfBuilder.newConf()
                .with(TopologyConfig.TOPOLOGY_NAME_CONFIG, "test-name")
                .with(TopologyConfig.TOPOLOGY_DESCRIPTION_CONFIG, "test-description")
                .with(TopologyConfig.TOPOLOGY_PROVIDER_ALIAS_CONFIG, "alias")
                .with(TopologyConfig.TOPOLOGY_STREAMS_CONFIG, Conf.with("configKey", "configValue"))
                .build();

        TopologyConfig topologyConfig = TopologyConfig.read(conf);

        assertEquals("test-name", topologyConfig.name().get());
        assertEquals("test-description", topologyConfig.description().get());
        assertEquals("alias", topologyConfig.type());
        assertTrue(topologyConfig.config().get().hasPath("configKey"));
    }

    @Test
    public void shouldThrowExceptionGivenConfWithMissingType() {

        MissingConfException exception = assertThrows(
            MissingConfException.class, () -> TopologyConfig.read(Conf.empty()));
        Assertions.assertEquals("Missing config parameter with name : topology", exception.getMessage());

    }
}
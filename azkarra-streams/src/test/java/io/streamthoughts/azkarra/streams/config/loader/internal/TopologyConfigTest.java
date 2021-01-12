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
package io.streamthoughts.azkarra.streams.config.loader.internal;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.MissingConfException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TopologyConfigTest {

    @Test
    public void shouldCreateTopologyConfigGivenValidConf() {

        var conf = Conf.of(
             TopologyConfig.TOPOLOGY_NAME_CONFIG, "test-name",
             TopologyConfig.TOPOLOGY_DESCRIPTION_CONFIG, "test-description",
             TopologyConfig.TOPOLOGY_PROVIDER_ALIAS_CONFIG, "alias",
             TopologyConfig.TOPOLOGY_STREAMS_CONFIG, Conf.of("configKey", "configValue")
        );

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
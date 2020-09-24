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
package io.streamthoughts.azkarra.runtime.streams;

import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DefaultApplicationIdBuilderTest {

    private DefaultApplicationIdBuilder builderWithUserEnv;
    private DefaultApplicationIdBuilder builderWithInternalEnv;
    
    @BeforeEach
    public void setUp() {
        builderWithUserEnv = new DefaultApplicationIdBuilder();
        StreamsExecutionEnvironment mock1 = Mockito.mock(StreamsExecutionEnvironment.class);
        Mockito.when(mock1.name()).thenReturn("test");
        builderWithUserEnv.setExecutionEnvironment(mock1);

        builderWithInternalEnv = new DefaultApplicationIdBuilder();
        StreamsExecutionEnvironment mock2 = Mockito.mock(StreamsExecutionEnvironment.class);
        Mockito.when(mock2.name()).thenReturn(DefaultAzkarraContext.DEFAULT_ENV_NAME);
        builderWithInternalEnv.setExecutionEnvironment(mock2);
    }

    @Test
    public void shouldReturnApplicationIdWhenConfigured() {
        final String applicationId = "user-defined-application-id";
        TopologyMetadata metadata = getTopologyMetadata("dummy");
        ApplicationId result = builderWithUserEnv.buildApplicationId(
            metadata,
            Conf.of("streams." + StreamsConfig.APPLICATION_ID_CONFIG, applicationId));
        Assertions.assertEquals(applicationId, result.toString());
    }

    @Test
    public void shouldGenerateIdUsingNameAndVersionPrefixedGivenUserEnv() {

        TopologyMetadata metadata = getTopologyMetadata("dummy");
        ApplicationId result = builderWithUserEnv.buildApplicationId(
            metadata,
            Conf.empty());
        Assertions.assertEquals("test-dummy-1-0", result.toString());
    }

    @Test
    public void shouldGenerateIdUsingNameAndVersionNotPrefixedGivenInternalEnv() {

        TopologyMetadata metadata = getTopologyMetadata("dummy");
        ApplicationId result = builderWithInternalEnv.buildApplicationId(
            metadata,
            Conf.empty());
        Assertions.assertEquals("dummy-1-0", result.toString());
    }

    @Test
    public void shouldGenerateNormalizedIdUsingNameAndVersionWhenNoOneIsConfigured() {

        TopologyMetadata metadata = getTopologyMetadata("example.package.MyTopology");
        ApplicationId result = builderWithUserEnv.buildApplicationId(
            metadata,
            Conf.empty());
        Assertions.assertEquals("test-example-package-my-topology-1-0", result.toString());
    }

    private TopologyMetadata getTopologyMetadata(final String dummy) {
        return new TopologyMetadata(dummy, "1.0", "-");
    }
}
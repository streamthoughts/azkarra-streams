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
package io.streamthoughts.azkarra.streams.context;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentDescriptorFactory;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentFactory;
import io.streamthoughts.azkarra.streams.config.AzkarraConf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext.DEFAULT_ENV_NAME;
import static io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AzkarraContextLoaderTest {

    public static final String TEST_ENV_NAME = "test";
    private AzkarraContext context;

    @BeforeEach
    public void setUp() {
        context = create(new DefaultComponentFactory(new DefaultComponentDescriptorFactory()));
    }

    @Test
    public void shouldInitializeContextWithEnvironmentGivenValidConfig() {
        AzkarraContextLoader.load(context, AzkarraConf.create("context-autoconfig.conf"));

        List<StreamsExecutionEnvironment> environments = context.environments();

        assertEquals(2, environments.size());

        List<String> names = environments.stream().map(StreamsExecutionEnvironment::name).collect(Collectors.toList());
        assertTrue(names.contains(DEFAULT_ENV_NAME));
        assertTrue(names.contains(TEST_ENV_NAME));
    }
}
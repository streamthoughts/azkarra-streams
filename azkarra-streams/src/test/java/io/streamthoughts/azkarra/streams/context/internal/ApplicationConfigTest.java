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
package io.streamthoughts.azkarra.streams.context.internal;

import io.streamthoughts.azkarra.api.config.Conf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ApplicationConfigTest {

    @Test
    public void shouldCreateContextConfGivenEmptyConf() {
        ApplicationConfig contextConfig = ApplicationConfig.read(Conf.empty());

        Assertions.assertNotNull(contextConfig.context());
        Assertions.assertNotNull(contextConfig.environments());
        Assertions.assertNotNull(contextConfig.components());
    }

    @Test
    public void shouldCreateContextGivenConfWithProviders() {

        final Conf conf = Conf.of(
            ApplicationConfig.CONTEXT_COMPONENT_CONFIG,
            Collections.singletonList("test")
        );
        ApplicationConfig contextConfig = ApplicationConfig.read(conf);

        Set<String> providers = contextConfig.components();
        Assertions.assertNotNull(providers);
        Assertions.assertEquals(1, providers.size());
    }

    @Test
    public void shouldCreateContextGivenConfWithEnv() {

        Conf envConf = Conf.of(EnvironmentConfig.ENVIRONMENT_NAME_CONFIG, "__default");

        final Conf conf = Conf.of(
            ApplicationConfig.CONTEXT_ENVIRONMENTS_CONFIG,
            Collections.singletonList(envConf));

        ApplicationConfig contextConfig = ApplicationConfig.read(conf);

        List<EnvironmentConfig> environmentConfigs = contextConfig.environments();
        Assertions.assertNotNull(environmentConfigs);
        Assertions.assertEquals(1, environmentConfigs.size());
    }

}
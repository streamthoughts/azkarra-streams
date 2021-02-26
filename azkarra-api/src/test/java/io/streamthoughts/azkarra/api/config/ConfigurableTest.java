
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
package io.streamthoughts.azkarra.api.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.streamthoughts.azkarra.api.config.Configurable.isConfigurable;


public class ConfigurableTest {

    @Test
    public void should_verify_when_object_is_configurable() {
        Assertions.assertFalse(isConfigurable(Object.class));
        Assertions.assertTrue(isConfigurable(((Configurable) configuration -> { }).getClass()));
    }

    @Test
    public void should_configure_given_configurable_object() {
        final AtomicBoolean isConfigured = new AtomicBoolean(false);
        Configurable configurable = configuration -> isConfigured.set(true);

        Configurable.mayConfigure(configurable, Conf.EMPTY);
        Assertions.assertTrue(isConfigured.get());
    }
}
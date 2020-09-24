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
package io.streamthoughts.azkarra.streams.autoconfigure;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.EnableAutoConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AutoConfigureTest {

    private static final String TEST_SYSTEM_PROPERTY_NAME = "azkarra.system-prop";
    private static final String TEST_CUSTOM_PROPERTY_NAME = "azkarra.custom-prop";

    private static final String TEST_PROPERTY_VALUE = "test-value";

    @Test
    public void shouldLoadDefaultConfigsFromSystem(final @TempDir Path tempDir) throws IOException {
        createTempConfigProperties(tempDir);
        final AzkarraApplication application = new AzkarraApplication() {
            public Class<?> getMainApplicationClass() {
                return ClassWithDefaultAutoConfig.class;
            }
        };
        new AutoConfigure().load(application);
        Conf configuration = application.getConfiguration();
        assertEquals(TEST_PROPERTY_VALUE, configuration.getString(TEST_SYSTEM_PROPERTY_NAME));
    }

    @Test
    public void shouldLoadCustomConfigs() {
        final AzkarraApplication application = new AzkarraApplication() {
            public Class<?> getMainApplicationClass() {
                return ClassWithCustomAutoConfig.class;
            }
        };
        new AutoConfigure().load(application);
        Conf configuration = application.getConfiguration();
        assertEquals(TEST_PROPERTY_VALUE, configuration.getString(TEST_CUSTOM_PROPERTY_NAME));
    }

    private void createTempConfigProperties(@TempDir Path tempDir) throws IOException {
        Path props = Files.createFile(tempDir.resolve("test.properties"));
        Files.writeString(props, TEST_SYSTEM_PROPERTY_NAME + "=" + TEST_PROPERTY_VALUE);
        System.setProperty("config.file", props.toString());
    }

    @EnableAutoConfig
    public static class ClassWithDefaultAutoConfig {

    }

    @EnableAutoConfig(name = "custom")
    public static class ClassWithCustomAutoConfig {

    }

}
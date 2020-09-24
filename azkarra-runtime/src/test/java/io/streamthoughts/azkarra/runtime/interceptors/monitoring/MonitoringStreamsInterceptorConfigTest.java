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
package io.streamthoughts.azkarra.runtime.interceptors.monitoring;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_INTERCEPTOR_ADVERTISED_SERVER_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_INTERCEPTOR_EXTENSIONS_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_INTERCEPTOR_INTERVAL_MS_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_INTERCEPTOR_INTERVAL_MS_DEFAULT;
import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_INTERCEPTOR_TOPIC_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_INTERCEPTOR_TOPIC_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MonitoringStreamsInterceptorConfigTest {

    @Test
    public void shouldGetDefaultConfigsGivenNoProps() {
        MonitoringStreamsInterceptorConfig config = new MonitoringStreamsInterceptorConfig(Conf.empty());
        assertEquals(MONITORING_INTERCEPTOR_INTERVAL_MS_DEFAULT, config.getIntervalMs());
        assertEquals(MONITORING_INTERCEPTOR_TOPIC_DEFAULT, config.getTopic());
        assertTrue(config.getAdvertisedServer().isEmpty());
        assertTrue(config.getExtensions().toAttributesExtensions().isEmpty());
    }

    @Test
    public void shouldGetProvidedAdvertisedConfig() {
        MonitoringStreamsInterceptorConfig config = new MonitoringStreamsInterceptorConfig(
                Conf.of(MONITORING_INTERCEPTOR_ADVERTISED_SERVER_CONFIG, "my-server")
        );
        assertEquals("my-server", config.getAdvertisedServer().get());
    }

    @Test
    public void shouldGetProvidedIntervalMsConfig() {
        MonitoringStreamsInterceptorConfig config = new MonitoringStreamsInterceptorConfig(
            Conf.of(MONITORING_INTERCEPTOR_INTERVAL_MS_CONFIG, 5000)
        );
        assertEquals(5000L, config.getIntervalMs());
    }

    @Test
    public void shouldGetProvidedTopicConfig() {
        MonitoringStreamsInterceptorConfig config = new MonitoringStreamsInterceptorConfig(
            Conf.of(MONITORING_INTERCEPTOR_TOPIC_CONFIG, "test-topic")
        );
        assertEquals("test-topic", config.getTopic());
    }

    @Test
    public void shouldGetProvidedExtensionsConfigs() {

        MonitoringStreamsInterceptorConfig config = new MonitoringStreamsInterceptorConfig(
            Conf.of(Map.of(
                MONITORING_INTERCEPTOR_EXTENSIONS_CONFIG + ".k1", "v1",
                MONITORING_INTERCEPTOR_EXTENSIONS_CONFIG + ".k2", "v2")
            ));

        Map<String, Object> extensions = config.getExtensions().toAttributesExtensions();
        assertNotNull(extensions);
        assertEquals(2, extensions.size());
        assertEquals("v1", extensions.get("k1"));
        assertEquals("v2", extensions.get("k2"));
    }
}
/*
 * Copyright 2021 StreamThoughts.
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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.KafkaMonitoringReporter;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.KafkaStreamsMetadata;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.MonitoringReporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

public class MonitoringStreamsInterceptorTest {

    @Test
    public void should_register_kafka_reporter_when_enabled() {
        MonitoringStreamsInterceptor interceptor = new MonitoringStreamsInterceptor();
        interceptor.setComponentFactory(Mockito.mock(ComponentFactory.class));
        Assertions.assertTrue(interceptor.reporters().isEmpty());
        interceptor.configure(Conf.empty());

        Assertions.assertEquals(1, interceptor.reporters().size());
        Assertions.assertEquals(KafkaMonitoringReporter.class, interceptor.reporters().get(0).getClass());
    }

    @Test
    public void should_not_register_kafka_reporter_when_disabled() {
        MonitoringStreamsInterceptor interceptor = new MonitoringStreamsInterceptor();
        interceptor.setComponentFactory(Mockito.mock(ComponentFactory.class));

        interceptor.configure(Conf.of(
                MonitoringStreamsInterceptorConfig.MONITORING_STREAMS_INTERCEPTOR_KAFKA_REPORTER_ENABLE_CONFIG,
                false
        ));

        Assertions.assertTrue(interceptor.reporters().isEmpty());
    }

    @Test
    public void should_register_reporters_components() {
        MonitoringStreamsInterceptor interceptor = new MonitoringStreamsInterceptor();
        ComponentFactory factory = Mockito.mock(ComponentFactory.class);
        Mockito.when(factory.getAllComponents(Mockito.eq(MonitoringReporter.class), Mockito.any()))
               .thenReturn(Collections.singletonList(new MockMonitoringReporter()));

        interceptor.setComponentFactory(factory);
        interceptor.configure(Conf.of(
                MonitoringStreamsInterceptorConfig.MONITORING_STREAMS_INTERCEPTOR_KAFKA_REPORTER_ENABLE_CONFIG,
                false
        ));
        Assertions.assertEquals(1, interceptor.reporters().size());
        Assertions.assertEquals(MockMonitoringReporter.class, interceptor.reporters().get(0).getClass());
    }

    private static final class MockMonitoringReporter implements MonitoringReporter {

        @Override
        public void report(KafkaStreamsMetadata metadata) {

        }
    }
}
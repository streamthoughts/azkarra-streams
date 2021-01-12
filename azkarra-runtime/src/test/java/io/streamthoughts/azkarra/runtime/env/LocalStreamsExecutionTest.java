/*
 * Copyright 2019-2021 StreamThoughts.
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

package io.streamthoughts.azkarra.runtime.env;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsTopologyMeta;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareTopologySupplier;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.streamthoughts.azkarra.runtime.interceptors.AutoCreateTopicsInterceptorConfig.AUTO_CREATE_TOPICS_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.WaitForSourceTopicsInterceptorConfig.WAIT_FOR_TOPICS_ENABLE_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LocalStreamsExecutionTest {

    private final DefaultAzkarraContext context = DefaultAzkarraContext.create(Conf.empty());

    private final ArgumentCaptor<Executed> executedArgumentCaptor = ArgumentCaptor.forClass(Executed.class);

    @Test
    public void should_properly_merged_all_configs_when_adding_topology() {
        //Setup
        var mkEnv = mock(LocalStreamsExecutionEnvironment.class);
        when(mkEnv.name()).thenReturn("test");
        when(mkEnv.getConfiguration()).thenReturn(Conf.of("prop.env", "env.value"));

        var mkDescriptor = mock(TopologyDescriptor.class);
        when(mkDescriptor.name()).thenReturn("test");
        when(mkDescriptor.configuration()).thenReturn(Conf.of("prop.descriptor", "desc.value"));
        when(mkDescriptor.type()).thenReturn(TopologyDescriptor.class);
        Executed executed = Executed.as("test-app").withConfig(Conf.of("prop.executed", "exec.value"));
        context.setConfiguration(Conf.of("prop.context", "value"));

        final LocalStreamsExecution execution = new LocalStreamsExecution(
            new StreamsTopologyMeta(
                mkDescriptor.name(),
                mkDescriptor.version(),
                mkDescriptor.description(),
                mkDescriptor.type(),
                mkDescriptor.classLoader(),
                mkDescriptor.configuration()
            ),
            executed,
            context,
            mkEnv
        );

        execution.start();

        // Assert
        verify(mkEnv, times(1)).addTopology(
            any(ContextAwareTopologySupplier.class),
            executedArgumentCaptor.capture()
        );

        var captured = new InternalExecuted(executedArgumentCaptor.getValue());
        assertEquals("test-app", captured.name());
        assertTrue(captured.config().hasPath("prop.env"), "Missing prop.env");
        assertTrue(captured.config().hasPath("prop.descriptor"), "Missing prop.descriptor");
        assertTrue(captured.config().hasPath("prop.executed"), "Missing prop.executed");
        assertTrue(captured.config().hasPath("prop.context"), "Missing prop.context");
    }

    @Test
    public void should_properly_configure_interceptors_when_adding_topology() {
        var mkEnv = mock(LocalStreamsExecutionEnvironment.class);
        when(mkEnv.name()).thenReturn("test");

        var mkDescriptor = mock(TopologyDescriptor.class);
        when(mkDescriptor.name()).thenReturn("test");
        when(mkDescriptor.configuration()).thenReturn(Conf.empty());
        when(mkDescriptor.type()).thenReturn(TopologyDescriptor.class);
        when(mkDescriptor.classLoader()).thenReturn(this.getClass().getClassLoader());

        context.setConfiguration(Conf.of(
            AUTO_CREATE_TOPICS_ENABLE_CONFIG, true,
            MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG, true,
            WAIT_FOR_TOPICS_ENABLE_CONFIG, true
        ));

        //Execute
        final LocalStreamsExecution execution = new LocalStreamsExecution(
            new StreamsTopologyMeta(
                mkDescriptor.name(),
                mkDescriptor.version(),
                mkDescriptor.description(),
                mkDescriptor.type(),
                mkDescriptor.classLoader(),
                mkDescriptor.configuration()
            ),
            Executed.as("test"),
            context,
            mkEnv
        );
        execution.start();

        // Assert
        verify(mkEnv, times(1)).addTopology(
            any(ContextAwareTopologySupplier.class),
            executedArgumentCaptor.capture()
        );

        var captured = new InternalExecuted(executedArgumentCaptor.getValue());
        captured.interceptors().forEach(it -> Configurable.mayConfigure(it, Conf.empty()));
        assertEquals(4, captured.interceptors().size());
        assertEquals("ClassloadingIsolationInterceptor", captured.interceptors().get(0).get().name()); // assert first
        assertEquals("WaitForSourceTopicsInterceptor", captured.interceptors().get(3).get().name()); // assert last
    }

}
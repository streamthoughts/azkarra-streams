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
package io.streamthoughts.azkarra.runtime.context;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.NoSuchComponentException;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsEnvironmentException;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.runtime.context.internal.ContextAwareTopologySupplier;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import io.streamthoughts.azkarra.runtime.interceptors.AutoCreateTopicsInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptor;
import io.streamthoughts.azkarra.runtime.interceptors.WaitForSourceTopicsInterceptor;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.function.Supplier;

import static io.streamthoughts.azkarra.runtime.interceptors.AutoCreateTopicsInterceptorConfig.AUTO_CREATE_TOPICS_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG;
import static io.streamthoughts.azkarra.runtime.interceptors.WaitForSourceTopicsInterceptorConfig.WAIT_FOR_TOPICS_ENABLE_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultAzkarraContextTest {

    private DefaultAzkarraContext context;

    private ArgumentCaptor<Executed> executedArgumentCaptor = ArgumentCaptor.forClass(Executed.class);

    @BeforeEach
    public void setUp() {
        // Create default context with empty configuration.
        context = (DefaultAzkarraContext) DefaultAzkarraContext.create();
    }

    @Test
    public void shouldRegisteredDefaultEnvironment() {
        List<StreamsExecutionEnvironment> environments = context.environments();
        assertEquals(1, environments.size());
        assertEquals(DefaultAzkarraContext.DEFAULT_ENV_NAME, environments.get(0).name());
    }

    @Test
    public void shouldRegisterTopologyToGivenEnvironmentWhenStart() {
        StreamsExecutionEnvironment env = spy(DefaultStreamsExecutionEnvironment.create("env"));
        context.addExecutionEnvironment(env);
        context.addTopology(TestTopologyProvider.class, "env", Executed.as("test"));
        context.preStart();
        verify(env, times(1))
                .addTopology(any(Supplier.class), executedArgumentCaptor.capture());

        InternalExecuted executed = new InternalExecuted(executedArgumentCaptor.getValue());
        assertEquals("test", executed.name());
    }

    @Test
    public void shouldThrowExceptionWhenAddingTopologyGivenUnknownEnvironment() {
        context.addTopology(TestTopologyProvider.class, "env", Executed.as("test"));
        InvalidStreamsEnvironmentException exception = Assertions.assertThrows(InvalidStreamsEnvironmentException.class, () -> {
            context.start();
        });

        String errorMessage = exception.getMessage();
        assertEquals("Error while adding topology '"
                + TestTopologyProvider.class.getName() + "', environment 'env' not found", errorMessage);
    }

    @Test
    public void shouldAutomaticallyRegisterConditionalAutoCreateTopicsInterceptor() {
        ComponentFactory factory = context.getComponentFactory();
        Assertions.assertTrue(factory.containsComponent(AutoCreateTopicsInterceptor.class));

        Assertions.assertNotNull(factory.getComponent(
            AutoCreateTopicsInterceptor.class,
            Conf.of(AUTO_CREATE_TOPICS_ENABLE_CONFIG, true))
        );
        Assertions.assertThrows(
            NoSuchComponentException.class,
            () -> factory.getComponent(
                AutoCreateTopicsInterceptor.class,
                Conf.of(AUTO_CREATE_TOPICS_ENABLE_CONFIG, false)
            )
        );
    }

    @Test
    public void shouldAutomaticallyRegisterConditionalMonitoringStreamsInterceptor() {
        ComponentFactory factory = context.getComponentFactory();
        Assertions.assertTrue(factory.containsComponent(MonitoringStreamsInterceptor.class));

        Assertions.assertNotNull(factory.getComponent(
            MonitoringStreamsInterceptor.class,
            Conf.of(MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG, true))
        );
        Assertions.assertThrows(
            NoSuchComponentException.class,
            () -> factory.getComponent(
                MonitoringStreamsInterceptor.class,
                Conf.of(MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG, false)
            )
        );
    }

    @Test
    public void shouldAutomaticallyRegisterConditionalWaitForSourceTopicsInterceptor() {
        ComponentFactory factory = context.getComponentFactory();
        Assertions.assertTrue(factory.containsComponent(WaitForSourceTopicsInterceptor.class));

        Assertions.assertNotNull(factory.getComponent(
            WaitForSourceTopicsInterceptor.class,
            Conf.of(WAIT_FOR_TOPICS_ENABLE_CONFIG, true))
        );
        Assertions.assertThrows(
            NoSuchComponentException.class,
            () -> factory.getComponent(
                WaitForSourceTopicsInterceptor.class,
                Conf.of(WAIT_FOR_TOPICS_ENABLE_CONFIG, false)
            )
        );
    }

    @Test
    public void shouldProperlyMergedAllConfigsWhenAddingTopology() {
        //Setup
        var mkEnv = mock(StreamsExecutionEnvironment.class);
        when(mkEnv.name()).thenReturn("test");
        when(mkEnv.getConfiguration()).thenReturn(Conf.of("prop.env", "env.value"));

        var mkDescriptor = mock(TopologyDescriptor.class);
        when(mkDescriptor.name()).thenReturn("test");
        when(mkDescriptor.configuration()).thenReturn(Conf.of("prop.descriptor", "desc.value"));
        Executed executed = Executed.as("test-app").withConfig(Conf.of("prop.executed", "exec.value"));
        context.setConfiguration(Conf.of("prop.context", "value"));

        //Execute
        context.addTopologyToEnvironment(mkDescriptor, mkEnv, new InternalExecuted(executed));

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
    public void shouldProperlyConfigureInterceptorsWhenAddingTopology() {
        var mkEnv = mock(StreamsExecutionEnvironment.class);
        when(mkEnv.name()).thenReturn("test");

        var mkDescriptor = mock(TopologyDescriptor.class);
        when(mkDescriptor.name()).thenReturn("test");
        when(mkDescriptor.configuration()).thenReturn(Conf.empty());
        when(mkDescriptor.classLoader()).thenReturn(this.getClass().getClassLoader());

        context.setConfiguration(Conf.of(
            AUTO_CREATE_TOPICS_ENABLE_CONFIG, true,
            MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG, true,
            WAIT_FOR_TOPICS_ENABLE_CONFIG, true
        ));

        //Execute
        context.addTopologyToEnvironment(mkDescriptor, mkEnv, new InternalExecuted(Executed.as("test-app")));
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

    public static class TestTopologyProvider implements TopologyProvider {

        @Override
        public String version() {
            return "1.0";
        }

        @Override
        public Topology topology() {
            return null;
        }
    }
}
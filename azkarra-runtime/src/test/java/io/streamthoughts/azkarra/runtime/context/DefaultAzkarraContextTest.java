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
import io.streamthoughts.azkarra.api.errors.InvalidStreamsEnvironmentException;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        StreamsExecutionEnvironment env = Mockito.spy(DefaultStreamsExecutionEnvironment.create("env"));
        context.addExecutionEnvironment(env);
        context.addTopology(TestTopologyProvider.class, "env", Executed.as("test"));
        context.preStart();
        Mockito.verify(env, Mockito.times(1))
                .addTopology(Mockito.any(Supplier.class), executedArgumentCaptor.capture());

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


    public static class TestTopologyProvider implements TopologyProvider {

        @Override
        public String version() {
            return "1.0";
        }

        @Override
        public Topology get() {
            return null;
        }
    }
}
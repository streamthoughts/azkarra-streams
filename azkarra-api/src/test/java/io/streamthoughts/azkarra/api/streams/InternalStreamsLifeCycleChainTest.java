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
package io.streamthoughts.azkarra.api.streams;

import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.streams.internal.InternalStreamsLifeCycleChain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InternalStreamsLifeCycleChainTest {

    private List<String> results;

    private List<StreamsLifecycleInterceptor> interceptors;

    @BeforeEach
    public void setUp() {
        results = new ArrayList<>();
        interceptors = new ArrayList<>();
        interceptors.add(new DummyStreamsLifeCycleInterceptor("A", results));
        interceptors.add(new DummyStreamsLifeCycleInterceptor("B", results));
        interceptors.add(new DummyStreamsLifeCycleInterceptor("C", results));
    }

    @Test
    public void shouldInvokeInterceptorsInOrderWhenCallingOnStart() {
        InternalStreamsLifeCycleChain chain = new InternalStreamsLifeCycleChain(
            interceptors.iterator(),
            (i, c) ->    i.onStart(null, c) ,
            () -> results.add("callback")
        );
        chain.execute();
        List<String> expected = Arrays.asList("A", "B", "C", "callback");
        Assertions.assertEquals(expected, results);
    }

    @Test
    public void shouldInvokeInterceptorsInOrderWhenCallingOnStop() {
        InternalStreamsLifeCycleChain chain = new InternalStreamsLifeCycleChain(
                interceptors.iterator(),
                (i, c) ->    i.onStop(null, c) ,
                () -> results.add("callback")
        );
        chain.execute();
        List<String> expected = Arrays.asList("A", "B", "C", "callback");
        Assertions.assertEquals(expected, results);
    }

    @Test
    public void shouldNotBreakTheChainWhenAExceptionIsThrown() {
        interceptors.add(1, new StreamsLifecycleInterceptor() {
            @Override
            public void onStart(StreamsLifecycleContext context, StreamsLifecycleChain chain) {
                throw new RuntimeException("Fail!");
            }
        });
        InternalStreamsLifeCycleChain chain = new InternalStreamsLifeCycleChain(
                interceptors.iterator(),
                (i, c) ->    i.onStart(null, c) ,
                () -> results.add("callback")
        );
        chain.execute();
        List<String> expected = Arrays.asList("A", "B", "C", "callback");
        Assertions.assertEquals(expected, results);
    }

    public static class DummyStreamsLifeCycleInterceptor implements StreamsLifecycleInterceptor {

        private final String id;
        private final List<String> collector;

        public DummyStreamsLifeCycleInterceptor(final String id, final List<String> collector) {
            this.id = id;
            this.collector = collector;
        }

        @Override
        public void onStart(final StreamsLifecycleContext context, final StreamsLifecycleChain chain) {
            collector.add(id);
            chain.execute();
        }

        @Override
        public void onStop(final StreamsLifecycleContext context, final StreamsLifecycleChain chain) {
            collector.add(id);
            chain.execute();
        }
    }
}
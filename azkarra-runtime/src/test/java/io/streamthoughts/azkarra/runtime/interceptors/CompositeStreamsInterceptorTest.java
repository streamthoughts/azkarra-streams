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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.streams.internal.InternalStreamsLifeCycleChain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CompositeStreamsInterceptorTest {

    private List<String> results;

    private List<StreamsLifecycleInterceptor> interceptors;

    @BeforeEach
    public void setUp() {
        results = new ArrayList<>();
        interceptors = new ArrayList<>();
        interceptors.add(new StreamsLifecycleInterceptor() {
            @Override
            public void onStart(StreamsLifecycleContext context, StreamsLifecycleChain chain) {
                results.add("A");
                chain.execute();
            }
        });
        interceptors.add(new StreamsLifecycleInterceptor() {
            @Override
            public void onStart(StreamsLifecycleContext context, StreamsLifecycleChain chain) {
                results.add("B");
                chain.execute();
            }
        });
    }

    @Test
    public void shouldInvokeInterceptorsInOrderWhenCallingOnStart() {

        CompositeStreamsInterceptor composite = new CompositeStreamsInterceptor(interceptors);
        InternalStreamsLifeCycleChain chain = new InternalStreamsLifeCycleChain(
            Collections.<StreamsLifecycleInterceptor>singletonList(new StreamsLifecycleInterceptor() {
                @Override
                public void onStart(StreamsLifecycleContext context, StreamsLifecycleChain chain) {
                    results.add("C");
                    chain.execute();
                }
            }).iterator(),
            (i, c) ->    i.onStart(null, c) ,
            () -> results.add("callback")
        );

        composite.onStart(null, chain);
        List<String> expected = Arrays.asList("A", "B", "C", "callback");
        Assertions.assertEquals(expected, results);
    }
}
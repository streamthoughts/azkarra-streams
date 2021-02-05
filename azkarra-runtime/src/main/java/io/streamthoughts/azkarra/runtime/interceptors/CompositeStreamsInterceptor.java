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

import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentAware;
import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.streams.internal.InternalStreamsLifeCycleChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CompositeStreamsInterceptor implements StreamsLifecycleInterceptor, StreamsExecutionEnvironmentAware {

    private final List<StreamsLifecycleInterceptor> interceptors;

    /**
     * Creates a new {@link CompositeStreamsInterceptor} instance.
     */
    public CompositeStreamsInterceptor() {
        this(new ArrayList<>());
    }

    /**
     * Creates a new {@link CompositeStreamsInterceptor} instance.
     *
     * @param interceptors  the list of interceptors.
     */
    public CompositeStreamsInterceptor(final Collection<StreamsLifecycleInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
    }

    public void addInterceptors(final Collection<StreamsLifecycleInterceptor> interceptors) {
        this.interceptors.addAll(interceptors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifecycleContext context, final StreamsLifecycleChain chain) {
        new InternalStreamsLifeCycleChain(
            interceptors.iterator(),
            (interceptor, c) -> interceptor.onStart(context, c),
            chain::execute
        ).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(final StreamsLifecycleContext context, final StreamsLifecycleChain chain) {
        new InternalStreamsLifeCycleChain(
            interceptors.iterator(),
            (interceptor, c) -> interceptor.onStop(context, c),
            chain::execute
        ).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExecutionEnvironment(final StreamsExecutionEnvironment<?> environment) {
        for (StreamsLifecycleInterceptor interceptor : interceptors) {
            if (interceptor instanceof StreamsExecutionEnvironmentAware) {
                ((StreamsExecutionEnvironmentAware)interceptor).setExecutionEnvironment(environment);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        if (interceptors.isEmpty()) {
            return "CompositeStreamsInterceptor[]";
        }
        if (interceptors.size() == 1) {
            return interceptors.get(0).name();
        }
        return interceptors.stream()
            .map(StreamsLifecycleInterceptor::name)
            .collect(Collectors.joining(", ", "CompositeStreamsInterceptor[", "]"));
    }
}

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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.StreamsLifeCycleChain;
import io.streamthoughts.azkarra.api.StreamsLifeCycleContext;
import io.streamthoughts.azkarra.api.StreamsLifeCycleInterceptor;
import io.streamthoughts.azkarra.api.util.ClassUtils;

import java.util.Objects;

public class ClassloadingIsolationInterceptor implements StreamsLifeCycleInterceptor {

    private final ClassLoader classLoader;

    /**
     * Creates a new {@link ClassloadingIsolationInterceptor} instance.
     *
     * @param classLoader   the {@link ClassLoader} to be used.
     */
    public ClassloadingIsolationInterceptor(final ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader cannot be null");
        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifeCycleContext context, final StreamsLifeCycleChain chain) {

        final ClassLoader saveLoader = ClassUtils.compareAndSwapLoaders(this.classLoader);
        try {
            chain.execute();
        } finally {
            ClassUtils.compareAndSwapLoaders(saveLoader);
        }
    }
}

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
package io.streamthoughts.azkarra.api.streams;

import io.streamthoughts.azkarra.api.StreamsLifeCycleChain;
import io.streamthoughts.azkarra.api.StreamsLifeCycleInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class InternalStreamsLifeCycleChain implements StreamsLifeCycleChain {

    private static final Logger LOG = LoggerFactory.getLogger(InternalStreamsLifeCycleChain.class);

    private final Callback callback;
    private final Iterator<StreamsLifeCycleInterceptor> interceptors;
    private final Runnable runnable;

    /**
     * Creates a new {@link InternalStreamsLifeCycleChain} instance.
     *
     * @param interceptors  the list of {@link StreamsLifeCycleInterceptor} instance.
     * @param callback      the {@link Callback} to invoke on each interceptor.
     * @param runnable      the {@link Runnable} to execute at the end of the chain.
     */
    InternalStreamsLifeCycleChain(
            final Iterator<StreamsLifeCycleInterceptor> interceptors,
            final Callback callback,
            final Runnable runnable) {
        this.interceptors = interceptors;
        this.callback = callback;
        this.runnable = runnable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (!interceptors.hasNext()) {
            runnable.run();
            return;
        }

        StreamsLifeCycleInterceptor interceptor = interceptors.next();
        try {
            callback.execute(interceptor, this);
        } catch (Throwable t) {
            LOG.error(
                "Unexpected error while executing interceptor '{}'. Ignored.",
                interceptor.getClass().getSimpleName(), t);
            execute();
        }
    }

    @FunctionalInterface
    public interface Callback {

        void execute(final StreamsLifeCycleInterceptor interceptor, final StreamsLifeCycleChain chain);
    }
}

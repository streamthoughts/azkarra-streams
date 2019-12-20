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
package io.streamthoughts.azkarra.runtime.context.internal;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.components.GettableComponent;
import io.streamthoughts.azkarra.runtime.interceptors.CompositeStreamsInterceptor;

import java.util.Collections;

public class ContextAwareLifecycleInterceptorSupplier
        extends ContextAwareGettableComponentSupplier<StreamsLifecycleInterceptor> {

    public ContextAwareLifecycleInterceptorSupplier(final AzkarraContext context,
                                                    final GettableComponent<StreamsLifecycleInterceptor> gettable) {
        super(context, gettable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsLifecycleInterceptor get() {
        // The components returned from the registry are already configured.
        // Thus, here we need to wrap the component into a non-configurable one so that the configure method
        // will be not invoke a second time by the StreamsExecutionEnvironment.
        return new CompositeStreamsInterceptor(Collections.singleton(super.get()));
    }
}

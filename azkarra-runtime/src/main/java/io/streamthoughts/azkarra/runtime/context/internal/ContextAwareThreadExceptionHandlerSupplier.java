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
import io.streamthoughts.azkarra.api.components.GettableComponent;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.errors.StreamThreadExceptionHandler;

public class ContextAwareThreadExceptionHandlerSupplier
        extends ContextAwareGettableComponentSupplier<StreamThreadExceptionHandler> {

    public ContextAwareThreadExceptionHandlerSupplier(final AzkarraContext context,
                                                      final GettableComponent<StreamThreadExceptionHandler> gettable) {
        super(context, gettable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamThreadExceptionHandler get() {
        // The components returned from the registry are already configured.
        // Thus, here we need to wrap the component into a non-configurable one so that the configure method
        // will be not invoke a second time by the StreamsExecutionEnvironment.
        return new DelegateStreamThreadExceptionHandler(super.get());
    }

    /**
     * A delegating {@link StreamThreadExceptionHandler} which is not {@link Configurable}.
     */
    public static class DelegateStreamThreadExceptionHandler implements StreamThreadExceptionHandler {

        private final StreamThreadExceptionHandler delegate;

        DelegateStreamThreadExceptionHandler(final StreamThreadExceptionHandler delegate) {
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handle(final KafkaStreamsContainer container,
                           final Thread streamThread,
                           final Throwable e) {
            delegate.handle(container, streamThread, e);

        }
    }
}

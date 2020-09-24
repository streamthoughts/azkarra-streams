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
package io.streamthoughts.azkarra.runtime.context.internal;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentAware;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.config.ConfigurableSupplier;
import io.streamthoughts.azkarra.api.events.EventStream;
import io.streamthoughts.azkarra.api.events.EventStreamProvider;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import org.apache.kafka.streams.Topology;

import java.util.Collections;
import java.util.List;

public class ContextAwareTopologySupplier extends ConfigurableSupplier<TopologyProvider> {

    private final ComponentDescriptor<? extends TopologyProvider>  descriptor;
    private final AzkarraContext context;

    /**
     * Creates a new {@link ContextAwareTopologySupplier} instance.
     *
     * @param context       the {@link AzkarraContext} instance.
     * @param descriptor    the {@link TopologyDescriptor} instance.
     */
    public ContextAwareTopologySupplier(final AzkarraContext context,
                                        final ComponentDescriptor<? extends TopologyProvider> descriptor) {
        this.descriptor = descriptor;
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopologyProvider get(final Conf configs) {
        final ComponentFactory factory = context.getComponentFactory();

        final TopologyProvider provider = factory.getComponent(
            descriptor.type(),
            configs,
            Qualifiers.byVersion(descriptor.version())
        );

        if (provider instanceof AzkarraContextAware) {
            ((AzkarraContextAware)provider).setAzkarraContext(context);
        }

        // The components returned from the registry may be already configured.
        // Thus, here we need to wrap the component into a non-configurable one so that the configure method
        // will be not invoke a second time by the StreamsExecutionEnvironment.
        return new ClassLoaderAwareTopologyProvider(provider, descriptor.classLoader());
    }

    /**
     * A delegating {@link TopologyProvider} which is not {@link Configurable}.
     */
    public static class ClassLoaderAwareTopologyProvider
            extends DelegatingExecutionEnvironmentAware<TopologyProvider>
            implements TopologyProvider, StreamsExecutionEnvironmentAware, EventStreamProvider {

        private final ClassLoader topologyClassLoader;

        ClassLoaderAwareTopologyProvider(final TopologyProvider delegate,
                                         final ClassLoader topologyClassLoader) {
            super(delegate);
            this.topologyClassLoader = topologyClassLoader;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String version() {
            return delegate.version();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Topology topology() {
            // Classloader have to swap because the topology may be loaded from external directory.
            final ClassLoader classLoader = ClassUtils.compareAndSwapLoaders(topologyClassLoader);
            try {
                return delegate.topology();
            } finally {
                ClassUtils.compareAndSwapLoaders(classLoader);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<EventStream> eventStreams() {
            if (delegate instanceof EventStreamProvider) {
                return ((EventStreamProvider)delegate).eventStreams();
            }
            return Collections.emptyList();
        }
    }
}

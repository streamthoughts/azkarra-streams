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
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.config.ConfigurableSupplier;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import org.apache.kafka.streams.Topology;

import java.util.Objects;

public class ContextAwareTopologySupplier extends ConfigurableSupplier<TopologyProvider> {

    private final TopologyDescriptor descriptor;
    private final AzkarraContext context;

    /**
     * Creates a new {@link ContextAwareTopologySupplier} instance.
     *
     * @param context       the {@link AzkarraContext} instance.
     * @param descriptor    the {@link TopologyDescriptor} instance.
     */
    public ContextAwareTopologySupplier(final AzkarraContext context,
                                 final TopologyDescriptor descriptor) {
        this.descriptor = descriptor;
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopologyProvider get(final Conf configs) {
        final ComponentRegistry registry = context.getComponentRegistry();

        final TopologyProvider provider = registry.getVersionedComponent(
                descriptor.className(),
                descriptor.version(),
                configs
        );

        if (provider instanceof AzkarraContextAware) {
            ((AzkarraContextAware)provider).setAzkarraContext(context);
        }

        if (Configurable.isConfigurable(provider.getClass())) {
            // The components returned from the registry are already configured.
            // Thus, here we need to wrap the component into a non-configurable one so that the configure method
            // will be not invoke a second time by the StreamsExecutionEnvironment.
            return new DelegateContextTopologyProvider(provider);
        }
        return provider;
    }

    /**
     * A delegating {@link TopologyProvider} which is not {@link Configurable}.
     */
    public static class DelegateContextTopologyProvider implements TopologyProvider {

        private final TopologyProvider delegate;

        DelegateContextTopologyProvider(final TopologyProvider delegate) {
            Objects.requireNonNull(delegate, "delegate cannot be null");
            this.delegate = delegate;
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
        public Topology get() {
            return delegate.get();
        }
    }
}

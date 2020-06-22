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

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsFactory;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;

import java.util.Objects;

public class ClassLoaderAwareKafkaStreamsFactory
        extends DelegatingExecutionEnvironmentAware<KafkaStreamsFactory>
        implements KafkaStreamsFactory {

    private final ClassLoader classLoader;

    /**
     * Creates a new {@link ClassLoaderAwareKafkaStreamsFactory} instance.
     *
     * @param delegate      the {@link KafkaStreamsFactory} to delagate.
     * @param classLoader   the {@link ClassLoader} to be used.
     */
    public ClassLoaderAwareKafkaStreamsFactory(final KafkaStreamsFactory delegate,
                                               final ClassLoader classLoader) {
        super(delegate);
        this.classLoader = Objects.requireNonNull(classLoader, "the class-loader cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KafkaStreams make(final Topology topology, final Conf streamsConfig) {
        // Classloader have to swap because the topology may be loaded from external directory.
        final ClassLoader current = ClassUtils.compareAndSwapLoaders(classLoader);
        try {
            return delegate.make(topology, streamsConfig);
        } finally {
            ClassUtils.compareAndSwapLoaders(current);
        }
    }
}

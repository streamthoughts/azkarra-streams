/*
 * Copyright 2019-2021 StreamThoughts.
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

package io.streamthoughts.azkarra.runtime;

import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentFactory;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsEnvironmentException;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class StreamsExecutionEnvironmentAbstractFactory {

    private final Map<String, StreamsExecutionEnvironmentFactory<?>> factoryByTypes;

    /**
     * Creates a new {@link StreamsExecutionEnvironmentAbstractFactory} instance.
     *
     * @param factories all the {@link StreamsExecutionEnvironmentFactory}.
     */
    @SuppressWarnings("unchecked")
    public StreamsExecutionEnvironmentAbstractFactory(final Collection<StreamsExecutionEnvironmentFactory> factories) {
        this.factoryByTypes = factories
            .stream()
            .collect(Collectors.toMap(StreamsExecutionEnvironmentFactory::type, it -> it));
    }

    /**
     * Creates a new {@link StreamsExecutionEnvironment} for the specified type using the given name and configuration.
     *
     * @param type  the environment type.
     * @param name  the environment name.
     * @param conf  the environment configuration.
     * @return      a new {@link StreamsExecutionEnvironment}.
     */
    public StreamsExecutionEnvironment<?> create(final String type, final String name, final Conf conf) {
        final StreamsExecutionEnvironmentFactory<?> factory = factoryByTypes.get(type);
        if (factory == null) {
            throw new InvalidStreamsEnvironmentException("Cannot find factory for environment type " + type);
        }
        return factory.create(name, conf);
    }

    /**
     * Adds a new {@link StreamsExecutionEnvironmentFactory} to this one.
     *
     * @param factory   the {@link StreamsExecutionEnvironmentFactory} to be added.
     */
    public void addFactory(final StreamsExecutionEnvironmentFactory<?> factory) {
        this.factoryByTypes.put(factory.type(), factory);
    }
}

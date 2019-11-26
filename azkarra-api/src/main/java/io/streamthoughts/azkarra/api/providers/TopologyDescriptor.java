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
package io.streamthoughts.azkarra.api.providers;

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;

/**
 *  A {@link ComponentDescriptor} for describing a {@link TopologyProvider} implementation.
 *
 * @param <T>   the {@link TopologyProvider} type.
 */
public final class TopologyDescriptor<T extends TopologyProvider> extends ComponentDescriptor<T> {

    private final String description;

    private final Conf streamConfigs;

    /**
     * Creates a new {@link TopologyDescriptor} instance.
     *
     * @param cls         the topology provider class.
     * @param version     the topology version.
     * @param description the topology description.
     */
    public TopologyDescriptor(final String version,
                              final Class<T> cls,
                              final String description) {
        this(version, cls, description, Conf.empty());
    }

    /**
     * Creates a new {@link TopologyDescriptor} instance.
     *
     * @param cls           the topology provider class.
     * @param version       the topology version.
     * @param description   the topology description.
     * @param streamConfigs the streams configuration.
     */
    public TopologyDescriptor(final String version,
                              final Class<T> cls,
                              final String description,
                              final Conf streamConfigs) {
        super(cls, version);
        this.description = description;
        this.streamConfigs = streamConfigs;
    }

    public String name() {
        return type().getSimpleName();
    }

    public String description() {
        return description;
    }

    public Conf streamsConfigs() {
        return streamConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[" +
                "name=" + className() +
                ", version=" + version() +
                ", aliases=" + aliases() +
                ", description=" + description +
                ']';
    }
}

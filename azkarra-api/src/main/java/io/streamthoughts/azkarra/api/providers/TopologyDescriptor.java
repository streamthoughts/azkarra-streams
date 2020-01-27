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

import io.streamthoughts.azkarra.api.annotations.DefaultStreamsConfig;
import io.streamthoughts.azkarra.api.annotations.TopologyInfo;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.SimpleComponentDescriptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.api.util.ClassUtils.getAllDeclaredAnnotationsByType;

/**
 *  A {@link SimpleComponentDescriptor} for describing a {@link TopologyProvider} implementation.
 *
 * @param <T>   the {@link TopologyProvider} type.
 */
public final class TopologyDescriptor<T extends TopologyProvider> extends SimpleComponentDescriptor<T> {

    private static final String TOPOLOGY_INFO_ATTRIBUTE = TopologyInfo.class.getSimpleName();

    private final String description;

    private final Conf streamConfigs;

    /**
     * Creates a new {@link TopologyDescriptor} instance.
     */
    public TopologyDescriptor(final ComponentDescriptor<T> descriptor) {
        super(descriptor);
        description = metadata().stringValue(TOPOLOGY_INFO_ATTRIBUTE, "description");
        String[] aliases = metadata().arrayValue(TOPOLOGY_INFO_ATTRIBUTE, "aliases");
        addAliases(new HashSet<>(Arrays. asList(aliases)));

        // Because DefaultStreamsConfig is a repeatable annotation is more straightforward
        // to directly lookup for the annotation instead of using attributes
        // that will contains the container annotation)
        Map<String, String> mapConfigs = getAllDeclaredAnnotationsByType(type(), DefaultStreamsConfig.class)
            .stream()
            .collect(Collectors.toMap(DefaultStreamsConfig::name, DefaultStreamsConfig::value));
        streamConfigs = Conf.with(mapConfigs);
    }

    public String description() {
        return description;
    }

    public Conf streamsConfigs() {
        return streamConfigs;
    }
}

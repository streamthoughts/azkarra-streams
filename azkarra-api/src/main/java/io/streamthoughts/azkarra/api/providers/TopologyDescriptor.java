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
import io.streamthoughts.azkarra.api.components.ComponentAttribute;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.SimpleComponentDescriptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *  A {@link SimpleComponentDescriptor} for describing a {@link TopologyProvider} implementation.
 *
 * @param <T>   the {@link TopologyProvider} type.
 */
public final class TopologyDescriptor<T extends TopologyProvider> extends SimpleComponentDescriptor<T> {

    private static final String TOPOLOGY_INFO_ATTRIBUTE = TopologyInfo.class.getSimpleName();
    private static final String STREAMS_CONFIG_ATTRIBUTE = DefaultStreamsConfig.class.getSimpleName();

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

        Collection<ComponentAttribute> attributes = metadata().attributesForName(STREAMS_CONFIG_ATTRIBUTE);
        Map<String, String> mapConfigs = attributes.stream()
            .map(attribute -> {
                String name = attribute.stringValue("name");
                String value = attribute.stringValue("value");
                return Tuple.of(name, value);
            })
            .collect(Collectors.toMap(Tuple::left, Tuple::right));
        streamConfigs = Conf.with(mapConfigs);
    }

    public String description() {
        return description;
    }

    public Conf streamsConfigs() {
        return streamConfigs;
    }
}

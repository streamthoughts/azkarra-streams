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
package io.streamthoughts.azkarra.runtime.components;

import io.streamthoughts.azkarra.api.annotations.DefaultStreamsConfig;
import io.streamthoughts.azkarra.api.annotations.TopologyInfo;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorFactory;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.api.util.ClassUtils.getAllDeclaredAnnotationByType;

public class TopologyDescriptorFactory implements ComponentDescriptorFactory<TopologyProvider> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentDescriptor<TopologyProvider> make(final Class<? extends TopologyProvider> type,
                                                      final String version) {
        final String description = getDescriptionFromAnnotation(type);
        final Conf streamsConfigs = getStreamsConfigsFromAnnotation(type);
        TopologyDescriptor descriptor = new TopologyDescriptor<>(version, type, description, streamsConfigs);
        descriptor.addAliases(getAliasesFromAnnotation(type));
        return descriptor;
    }

    private Set<String> getAliasesFromAnnotation(final Class<? extends TopologyProvider> cls) {
        return getAllDeclaredAnnotationByType(cls, TopologyInfo.class)
            .stream()
            .flatMap(info -> Arrays.stream(info.aliases()))
            .collect(Collectors.toSet());
    }

    private String getDescriptionFromAnnotation(final Class<? extends TopologyProvider> cls) {
        return getAllDeclaredAnnotationByType(cls, TopologyInfo.class)
                .stream()
                .map(TopologyInfo::description)
                .findFirst().orElse(null);
    }

    private Conf getStreamsConfigsFromAnnotation(final Class<? extends TopologyProvider> cls) {
        return Conf.with(getAllDeclaredAnnotationByType(cls, DefaultStreamsConfig.class)
            .stream()
            .collect(Collectors.toMap(DefaultStreamsConfig::name, DefaultStreamsConfig::value))
        );
    }
}
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

package io.streamthoughts.azkarra.runtime.service;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.Qualifier;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.errors.NoSuchComponentException;
import io.streamthoughts.azkarra.api.model.TopologyAndAliases;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.util.Version;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractAzkarraStreamsService implements AzkarraStreamsService {

    protected final AzkarraContext context;
    /**
     * Creates a new {@link AbstractAzkarraStreamsService} instance.
     * @param context   the {@link AzkarraContext} instance.
     */
    AbstractAzkarraStreamsService(final AzkarraContext context) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<TopologyDescriptor> getTopologyProviders() {
        return context.topologyProviders();
    }

    public List<TopologyAndAliases> getAllTopologies() {
        Map<String, Set<String>> topologies = new HashMap<>();
        var factory = context.getComponentFactory();
        var descriptors = factory.findAllDescriptorsByClass(TopologyProvider.class);

        for (ComponentDescriptor<TopologyProvider> descriptor : descriptors) {
            TopologyDescriptor<?> topology = new TopologyDescriptor<>(descriptor);
            Set<String> aliases = topologies.get(topology.className());
            if (aliases == null) {
                aliases = topology.aliases();
            } else {
                aliases.retainAll(topology.aliases());
            }
            topologies.put(topology.className(), aliases);
        }

        return topologies.entrySet()
            .stream()
            .map(entry -> new TopologyAndAliases(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopologyDescriptor getTopologyByAliasAndVersion(final String alias,
                                                           final String version) {
        Objects.requireNonNull(alias, "alias cannot be null");
        Objects.requireNonNull(version, "version cannot be null");

        Qualifier<? extends TopologyProvider> qualifier = version.equalsIgnoreCase("latest")
                ? Qualifiers.byLatestVersion() : Qualifiers.byVersion(version);
        return getTopologyByAliasAndQualifiers(alias, qualifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public TopologyDescriptor getTopologyByAliasAndQualifiers(final String alias,
                                                              final Qualifier<? extends TopologyProvider> qualifier) {
        ComponentFactory factory = context.getComponentFactory();
        var optional = factory.findDescriptorByAlias(alias, qualifier);
        if (optional.isEmpty()) {
            throw NoSuchComponentException.forAliasAndQualifier(alias, qualifier);
        }

        var descriptor = optional.get();
        if (! TopologyProvider.class.isAssignableFrom(descriptor.type())) {
            throw new NoSuchComponentException(
                "Component for alias '"+ alias + "' and '" + qualifier + "' is not a sub-type of TopologyProvider");
        }
        return new TopologyDescriptor(descriptor);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Version> getTopologyVersionsByAlias(final String alias) {
        Objects.requireNonNull(alias, "alias cannot be null");
        ComponentFactory factory = context.getComponentFactory();
        Collection<ComponentDescriptor<Object>> descriptors = factory.findAllDescriptorsByAlias(alias);
        if (descriptors.isEmpty()) {
            throw NoSuchComponentException.forAlias(alias);
        }

        return descriptors
            .stream()
            .map(ComponentDescriptor::version)
            .sorted()
            .collect(Collectors.toList());
    }
}

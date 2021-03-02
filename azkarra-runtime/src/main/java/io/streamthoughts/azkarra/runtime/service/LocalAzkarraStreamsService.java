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

import io.streamthoughts.azkarra.api.ApplicationId;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.ContainerId;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentFactory;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsEnvironmentException;
import io.streamthoughts.azkarra.api.errors.NotFoundException;
import io.streamthoughts.azkarra.api.model.HasId;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsApplication;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.runtime.env.internal.BasicContainerId;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The default {@link AzkarraStreamsService} implementations.
 */
public class LocalAzkarraStreamsService extends AbstractAzkarraStreamsService {

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> listAllKafkaStreamsContainerIds() {
        return context.getAllEnvironments()
            .stream()
            .flatMap(environment -> HasId.getIds(environment.getContainerIds()).stream())
            .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> listAllKafkaStreamsApplicationIds() {
        return context.getAllEnvironments()
            .stream()
            .flatMap(environment -> HasId.getIds(environment.getApplicationIds()).stream())
            .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KafkaStreamsContainer getStreamsContainerById(final String containerId) {
        final Optional<KafkaStreamsContainer> container = context
            .getAllEnvironments()
            .stream()
            .flatMap(environment -> environment.getContainers().stream())
            .filter(o -> o.containerId().equals(containerId))
            .findFirst();

        if (container.isPresent()) {
            return container.get();
        }
        throw new NotFoundException("Failed to find Kafka Streams instance for container id '" + containerId + "'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<KafkaStreamsContainer> getAllStreamsContainersById(final String applicationId) {
        var environment = getStreamsApplicationById(applicationId).environment();
        return context.getEnvironmentForName(environment).getContainers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId startStreamsTopology(final String topologyType,
                                            final String topologyVersion,
                                            final String env,
                                            final Executed executed) {
        return context.addTopology(topologyType, topologyVersion, env, executed).orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getContextConfig() {
        return context.getConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNewEnvironment(final String name, final String type, final Conf conf) {
        var factories = context.getAllComponents(StreamsExecutionEnvironmentFactory.class);
        var opt = factories
            .stream()
            .filter(factory -> factory.type().equals(type))
            .findAny();
        if (opt.isEmpty()) {
            throw new InvalidStreamsEnvironmentException("Cannot find factory for environment type " + type);
        }
        context.addExecutionEnvironment(opt.get().create(name, conf));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KafkaStreamsApplication getStreamsApplicationById(final String id) {
        return context.getAllEnvironments()
           .stream()
           .flatMap(env -> env.getApplicationById(new ApplicationId(id)).stream())
           .findFirst()
           .orElseThrow(() -> new NotFoundException("Failed to find KafkaStreams application for id " + id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopStreamsContainer(final String containerId, final boolean cleanUp) {
        Objects.requireNonNull(containerId, "Cannot stop streams for an empty container id");
        getStreamsContainerById(containerId).close(cleanUp, Duration.ZERO); // non-blocking
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restartStreamsContainer(final String containerId) {
        Objects.requireNonNull(containerId, "Cannot restart streams for an empty container id");
        getStreamsContainerById(containerId).restart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void terminateStreamsContainer(final String containerId) {
        Objects.requireNonNull(containerId, "Cannot stop streams for an empty container id");

        final ContainerId id = new BasicContainerId(containerId);
        StreamsExecutionEnvironment<?> env = null;
        Iterator<StreamsExecutionEnvironment<?>> it = context.getAllEnvironments().iterator();
        while (it.hasNext() && env == null) {
            StreamsExecutionEnvironment<?> e = it.next();
            if (e.getContainerIds().contains(id)) {
                env = e;
            }
        }
        if (env != null) {
            env.terminate(id);
        } else {
            throw new NotFoundException(
                "Failed to find an environment running KafkaStreams containers for container id '" + id + "'."
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void terminateStreamsApplication(final String applicationId) {
        final ApplicationId id = new ApplicationId(applicationId);

        StreamsExecutionEnvironment<?> env = null;
        Iterator<StreamsExecutionEnvironment<?>> it = context.getAllEnvironments().iterator();
        while (it.hasNext() && env == null) {
            StreamsExecutionEnvironment<?> e = it.next();
            if (e.getApplicationIds().contains(id)) {
                env = e;
            }
        }
        if (env != null) {
            env.terminate(id);
        } else {
            throw new NotFoundException(
                "Failed to find an environment running KafkaStreams containers for application.id '"
                + applicationId + "'."
            );
        }
    }
}

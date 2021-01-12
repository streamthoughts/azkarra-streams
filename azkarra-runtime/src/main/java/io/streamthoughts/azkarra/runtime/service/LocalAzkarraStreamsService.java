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

import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentFactory;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsEnvironmentException;
import io.streamthoughts.azkarra.api.errors.NotFoundException;
import io.streamthoughts.azkarra.api.model.Environment;
import io.streamthoughts.azkarra.api.model.Metric;
import io.streamthoughts.azkarra.api.model.MetricGroup;
import io.streamthoughts.azkarra.api.model.StreamsStatus;
import io.streamthoughts.azkarra.api.model.StreamsTopologyGraph;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.ServerMetadata;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerGroupOffsets;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The default {@link AzkarraStreamsService} implementations.
 */
public class LocalAzkarraStreamsService extends AbstractAzkarraStreamsService {

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getAllStreams() {
        return context.getAllEnvironments()
            .stream()
            .flatMap(environment -> environment.applicationIds().stream())
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getStreamsConfigById(final String applicationId) {
        return getStreamsById(applicationId).streamsConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KafkaStreamsContainer getStreamsById(final String applicationId) {
        final Optional<KafkaStreamsContainer> container = context
            .getAllEnvironments()
            .stream()
            .flatMap(environment -> environment.applications().stream())
            .filter(o -> o.applicationId().equals(applicationId))
            .findFirst();

        if (container.isPresent()) {
            return container.get();
        }
        throw new NotFoundException("no Kafka Streams instance for application.id '" + applicationId + "'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsStatus getStreamsStatusById(final String applicationId) {
        KafkaStreamsContainer streams = getStreamsById(applicationId);
        return new StreamsStatus(
            streams.applicationId(),
            streams.state().value().name(),
            streams.threadMetadata());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamsTopologyGraph getStreamsTopologyById(final String applicationId) {
        return getStreamsById(applicationId).topologyGraph();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationId startStreamsTopology(final String topologyType,
                                              final String topologyVersion,
                                              final String env,
                                              final Executed executed) {
        return context.addTopology(topologyType, topologyVersion, env, executed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<MetricGroup> getStreamsMetricsById(final String applicationId) {
        return getStreamsById(applicationId).metrics();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<MetricGroup> getStreamsMetricsById(final String applicationId,
                                                  final Predicate<Tuple<String, Metric>> filter) {
        return getStreamsById(applicationId).metrics(KafkaStreamsContainer.KafkaMetricFilter.of(filter));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerGroupOffsets getStreamsConsumerOffsetsById(final String applicationId) {
        final KafkaStreamsContainer container = getStreamsById(applicationId);
        return container.offsets();
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
    public Set<Environment> getAllEnvironments() {
        return context.getAllEnvironments()
            .stream()
            .map( env -> new Environment(
                env.name(),
                env.type(),
                env.state(),
                env.getConfiguration().getConfAsMap(),
                env.applicationIds(),
                env.isDefault()
            )
          ).collect(Collectors.toSet());
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
    public Set<ServerMetadata> getStreamsInstancesById(final String applicationId) {
        final KafkaStreamsContainer container = getStreamsById(applicationId);
        return container.allMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopStreams(final String applicationId, final boolean cleanUp) {
        Objects.requireNonNull(applicationId, "cannot stop streams for an empty application.id");
        final KafkaStreamsContainer container = getStreamsById(applicationId);
        container.close(cleanUp, Duration.ZERO); // non-blocking
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restartStreams(final String applicationId) {
        Objects.requireNonNull(applicationId, "cannot restart streams for an empty application.id");
        final KafkaStreamsContainer container = getStreamsById(applicationId);
        container.restart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteStreams(final String applicationId) {

        StreamsExecutionEnvironment<?> env = null;
        Iterator<StreamsExecutionEnvironment<?>> it = context.getAllEnvironments().iterator();
        while (it.hasNext() && env == null) {
            StreamsExecutionEnvironment<?> e = it.next();
            boolean exists = e.applications()
                .stream()
                .map(KafkaStreamsContainer::applicationId)
                .collect(Collectors.toList())
                .contains(applicationId);
            if (exists) {
                env = e;
            }
        }
        if (env != null) {
            env.remove(new ApplicationId(applicationId));
        } else {
            throw new NotFoundException(
                "Can't find streams environment running an application with id'" + applicationId+ "'.");
        }
    }
}

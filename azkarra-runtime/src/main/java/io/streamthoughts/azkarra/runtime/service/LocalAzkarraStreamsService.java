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
package io.streamthoughts.azkarra.runtime.service;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.Error;
import io.streamthoughts.azkarra.api.errors.NotFoundException;
import io.streamthoughts.azkarra.api.model.Environment;
import io.streamthoughts.azkarra.api.model.Metric;
import io.streamthoughts.azkarra.api.model.MetricGroup;
import io.streamthoughts.azkarra.api.model.StreamsStatus;
import io.streamthoughts.azkarra.api.model.StreamsTopologyGraph;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.query.DistributedQuery;
import io.streamthoughts.azkarra.api.query.Queried;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.RemoteQueryClient;
import io.streamthoughts.azkarra.api.query.internal.Query;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.StreamsServerInfo;
import io.streamthoughts.azkarra.api.time.Time;
import io.streamthoughts.azkarra.runtime.env.DefaultStreamsExecutionEnvironment;
import org.apache.kafka.common.MetricName;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The default {@link AzkarraStreamsService} implementations.
 */
public class LocalAzkarraStreamsService implements AzkarraStreamsService {

    private final AzkarraContext context;

    private RemoteQueryClient remoteQueryClient;

    /**
     * Creates a new {@link LocalAzkarraStreamsService} instance.
     *
     * @param context  the {@link AzkarraContext} instance.
     * @param context  the {@link RemoteQueryClient} instance.
     */
    public LocalAzkarraStreamsService(final AzkarraContext context,
                                      final RemoteQueryClient remoteQueryClient) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(remoteQueryClient, "remoteQueryClient cannot be null");
        this.context = context;;
        this.remoteQueryClient = remoteQueryClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getAllStreams() {
        return containers()
            .stream()
            .map(KafkaStreamsContainer::applicationId)
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
        final Optional<KafkaStreamsContainer> container = containers()
            .stream()
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
        KafkaStreamsContainer streams = getStreamsById(applicationId);
        return StreamsTopologyGraph.build(streams.topologyDescription());
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
    public Set<TopologyDescriptor> getTopologyProviders() {
        return context.topologyProviders();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<MetricGroup> getStreamsMetricsById(final String applicationId) {
        return getStreamsMetricsById(applicationId, m -> true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<MetricGroup> getStreamsMetricsById(final String applicationId,
                                                  final Predicate<Tuple<String, Metric>> filter) {
        KafkaStreamsContainer container = getStreamsById(applicationId);

        Map<MetricName, ? extends org.apache.kafka.common.Metric> metrics = container.metrics();

        Map<String, List<Metric>> m = new HashMap<>(metrics.size());
        for (Map.Entry<MetricName, ? extends org.apache.kafka.common.Metric> elem : metrics.entrySet()) {
            final MetricName metricName = elem.getKey();
            final org.apache.kafka.common.Metric metricValue = elem.getValue();

            final Metric metric = new Metric(
                metricName.name(),
                metricName.group(),
                metricName.description(),
                metricName.tags(),
                metricValue.metricValue()
            );
            final boolean filtered = filter.test(Tuple.of(metricName.group(), metric));
            if (filtered) {
                m.computeIfAbsent(metricName.group(), k -> new LinkedList<>()).add(metric);
            }
        }

        return m.entrySet()
                .stream()
                .map(e -> new MetricGroup(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
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
        StreamsExecutionEnvironment defaultEnv = context.defaultExecutionEnvironment();
        return context.environments()
              .stream()
              .map( env -> new Environment(
                  env.name(),
                  env.state(),
                  env.getConfiguration().getConfAsMap(),
                  env.applications().stream().map(KafkaStreamsContainer::applicationId).collect(Collectors.toSet()),
                  env.name().equals(defaultEnv.name())
                  )
              ).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNewEnvironment(final String name, final Conf conf) {
        this.context.addExecutionEnvironment(DefaultStreamsExecutionEnvironment.create(conf, name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<StreamsServerInfo> getStreamsInstancesById(final String applicationId) {
        final KafkaStreamsContainer container = getStreamsById(applicationId);
        return container.getAllMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopStreams(final String applicationId, final boolean cleanUp) {
        Objects.requireNonNull(applicationId, "cannot stop streams for an empty application.id");
        final KafkaStreamsContainer container = getStreamsById(applicationId);
        container.close(cleanUp);
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

        StreamsExecutionEnvironment env = null;
        Iterator<StreamsExecutionEnvironment> it = context.environments().iterator();
        while (it.hasNext() && env == null) {
            StreamsExecutionEnvironment e = it.next();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> QueryResult<K, V> query(final String applicationId,
                                          final Query<K, V> query,
                                          final QueryParams parameters,
                                          final Queried options) {
        long now = Time.SYSTEM.milliseconds();

        final KafkaStreamsContainer container = getStreamsById(applicationId);

        Optional<StreamsServerInfo> server = container.getLocalServerInfo();

        final String localServerName = server.map(StreamsServerInfo::hostAndPort).orElse("N/A");

        final Optional<List<Error>> errors = query.validate(parameters);
        if (errors.isPresent()) {
            QueryResultBuilder<K, V> queryBuilder = QueryResultBuilder.newBuilder();
            return queryBuilder
            .setServer(localServerName)
            .setTook(Time.SYSTEM.milliseconds() - now)
            .setStatus(QueryStatus.INVALID)
            .setFailedResultSet(new ErrorResultSet(localServerName, false, QueryError.allOf(errors.get())))
            .build();
        }

        final DistributedQuery<K, V> distributed = new DistributedQuery<>(remoteQueryClient, query.prepare(parameters));
        return distributed.query(container, options);
    }

    private Collection<KafkaStreamsContainer> containers() {
        return context.environments().stream()
                .flatMap(environment -> environment.applications().stream())
                .collect(Collectors.toList());
    }
}

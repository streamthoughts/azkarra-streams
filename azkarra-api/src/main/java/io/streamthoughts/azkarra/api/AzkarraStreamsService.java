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
package io.streamthoughts.azkarra.api;

import io.streamthoughts.azkarra.api.components.Qualifier;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.NoSuchComponentException;
import io.streamthoughts.azkarra.api.errors.NotFoundException;
import io.streamthoughts.azkarra.api.model.Environment;
import io.streamthoughts.azkarra.api.model.Metric;
import io.streamthoughts.azkarra.api.model.MetricGroup;
import io.streamthoughts.azkarra.api.model.StreamsStatus;
import io.streamthoughts.azkarra.api.model.StreamsTopologyGraph;
import io.streamthoughts.azkarra.api.model.TopologyAndAliases;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.ApplicationId;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.ServerMetadata;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerGroupOffsets;
import io.streamthoughts.azkarra.api.util.Version;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The {@link AzkarraStreamsService} serves as the main front-facing interface for manipulating streams applications.
 */
public interface AzkarraStreamsService {

    /**
     * Returns the list of all running streams applications.
     *
     * @see StreamsConfig#APPLICATION_ID_CONFIG
     *
     * @return  a list of string ids.
     */
    Collection<String> getAllStreams();

    /**
     * Returns the {@link io.streamthoughts.azkarra.api.config.Conf} for the specified streams application.
     *
     * @param   applicationId the streams application id.
     * @return  a {@link Optional} of {@link Conf}.
     *
     * @throws NotFoundException  if not application exists for the id.
     */
    Conf getStreamsConfigById(final String applicationId);

    /**
     * Returns the {@link KafkaStreamsContainer} for the specified streams application.
     *
     * @param   applicationId the streams application id.
     * @return  a {@link KafkaStreamsContainer} instance.
     *
     * @throws NotFoundException  if not application exists for the id.
     */
    KafkaStreamsContainer getStreamsById(final String applicationId);

    /**
     * Returns the status for the specified streams application.
     *
     * @param   applicationId the streams application id.
     * @return  a {@link StreamsStatus} instance.
     *
     * @throws NotFoundException  if not application exists for the id.
     */
    StreamsStatus getStreamsStatusById(final String applicationId);

    /**
     * Returns the {@link StreamsTopologyGraph} for the specified streams application.
     *
     * @param   applicationId the streams application id.
     * @return  a {@link StreamsTopologyGraph} instance.
     *
     * @throws NotFoundException  if not application exists for the id.
     */
    StreamsTopologyGraph getStreamsTopologyById(final String applicationId);

    /**
     * Creates and starts a new streams job for the specified topology into the specified environment.
     *
     * @param topologyType      the topology type.
     * @param topologyVersion   the topology topologyVersion.
     * @param env               the environment name.
     * @param executed          the {@link Executed} instance.
     *
     * @return                  the streams application.id
     */
    ApplicationId startStreamsTopology(final String topologyType,
                                       final String topologyVersion,
                                       final String env,
                                       final Executed executed);

    /**
     * Gets all topologies available locally.
     *
     * @return  a set of {@link TopologyDescriptor} instance.
     */
    Set<TopologyDescriptor> getTopologyProviders();

    /**
     * Gets the list of all topologies.
     *
     * @return  the list {@link TopologyAndAliases}.
     */
    List<TopologyAndAliases> getAllTopologies();

    /**
     * Gets the {@link TopologyDescriptor} for the specified alias and version.
     *
     * @param alias     the topology alias.
     * @param version   the topology version.
     * @return          the {@link TopologyDescriptor}.
     * @throws NoSuchComponentException  if no topology exist for the given parameters.
     * @throws IllegalArgumentException  if the component for the given parameters is not a Topology.
     */
    TopologyDescriptor getTopologyByAliasAndVersion(final String alias, final String version);

    /**
     * Gets the {@link TopologyDescriptor} for the specified alias and qualifier.
     *
     * @param alias     the topology alias.
     * @param qualifier the topology qualifier.
     * @return          the {@link TopologyDescriptor}.
     * @throws NoSuchComponentException  if no topology exist for the given parameters.
     * @throws IllegalArgumentException  if the component for the given parameters is not a Topology.
     */
    TopologyDescriptor getTopologyByAliasAndQualifiers(final String alias,
                                                       final Qualifier<? extends TopologyProvider> qualifier);

    /**
     * Gets all versions of {@link TopologyDescriptor} for the specified alias.
     *
     * @param alias     the topology alias.
     * @throws NoSuchComponentException  if no topology exist for the given parameters.
     */
    List<Version> getTopologyVersionsByAlias(final String alias);

    /**
     * Gets all metrics for the specified streams application.
     *
     * @param applicationId the streams application id.
     *
     * @return              a set of {@link MetricGroup} instance.
     *
     * @throws NotFoundException  if not application exists for the id.
     */
    Set<MetricGroup> getStreamsMetricsById(final String applicationId);

    /**
     * Gets metrics for the specified streams application matching a predicate.
     *
     * @param applicationId the streams application id.
     * @param filter        the {@link Predicate} to be used for filtering {@link Metric}.
     *
     * @return              a set of {@link MetricGroup} instance.
     */
    Set<MetricGroup> getStreamsMetricsById(final String applicationId, final Predicate<Tuple<String, Metric>> filter);

    /**
     * Gets the topic/partitions offsets for the specified streams application.
     *
     * @param applicationId the streams application id.
     * @return              the {@link ConsumerGroupOffsets}.
     */
    ConsumerGroupOffsets getStreamsConsumerOffsetsById(final String applicationId);

    Conf getContextConfig();

    /**
     * Gets all existing streams environments.
     *
     * @return  a set of {@link Environment} instance.
     */
    Set<Environment> getAllEnvironments();

    /**
     * Gets all supported environment types.
     *
     * @return  the set of the environment types.
     */
    Set<String> getSupportedEnvironmentTypes();

    /**
     * Adds a new environment to this application.
     *
     * @param name  the environment name.
     * @param type  the environment type.
     * @param conf  the environment configuration.
     */
    void addNewEnvironment(final String name, final String type, final Conf conf);

    /**
     * Gets all local and remote streams instances for the specified streams application.
     *
     * @param applicationId the streams application id.
     * @return              the set of {@link ServerMetadata} instances.
     *
     * @throws NotFoundException  if not application exists for the id.
     */
    Set<ServerMetadata> getStreamsInstancesById(final String applicationId);

    /**
     * Stops the streams instance for the specified streams application.
     *
     * @param applicationId the streams application id.
     * @param cleanUp       the flag to indicate if the local streams states should be cleaned up.
     *
     * @throws NotFoundException  if not application exists for the id.
     */
    void stopStreams(final String applicationId, final boolean cleanUp);

    /**
     * Restarts the streams instance for the specified streams application.
     *
     * @param applicationId the streams application id.
     *
     * @throws NotFoundException  if not application exists for the id.
     */
    void restartStreams(final String applicationId);

    /**
     * Deletes the streams instance for the specified streams application.
     *
     * @param applicationId the streams application id.
     *
     * @throws NotFoundException  if not application exists for the id.
     */
    void deleteStreams(final String applicationId);
}

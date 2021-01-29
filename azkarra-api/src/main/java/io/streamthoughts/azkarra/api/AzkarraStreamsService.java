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
import io.streamthoughts.azkarra.api.model.TopologyAndAliases;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsApplication;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.util.Version;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The {@code AzkarraStreamsService} serves as the main front-facing interface for manipulating streams applications.
 */
public interface AzkarraStreamsService {

    /**
     * Returns the list of ids for all running Kafka Streams containers.
     *
     * @return a list of string ids.
     */
    Set<String> listAllKafkaStreamsContainerIds();

    /**
     * Returns the list of ids for all running Kafka Streams application.
     *
     * @return a list of string ids.
     * @see StreamsConfig#APPLICATION_ID_CONFIG
     */
    Set<String> listAllKafkaStreamsApplicationIds();

    /**
     * Returns the {@link KafkaStreamsContainer} for the specified id.
     *
     * @param containerId the container id.
     * @return a {@link KafkaStreamsContainer} instance.
     * @throws NotFoundException if no Kafka Streams instance exists for the id.
     */
    KafkaStreamsContainer getStreamsContainerById(final String containerId);

    /**
     * Returns all the {@link KafkaStreamsContainer} for the specified {@code application.id}.
     *
     * @param applicationId the {@code application.id}.
     * @return a {@link KafkaStreamsContainer} instance.
     * @throws NotFoundException if no Kafka Streams instance exists for the id.
     */
    Collection<KafkaStreamsContainer> getAllStreamsContainersById(final String applicationId);

    /**
     * Gets all local and remote streams instances for the specified streams application.
     *
     * @param applicationId the streams application id.
     * @return the {@link KafkaStreamsApplication} instance..
     * @throws NotFoundException if not application exists for the id.
     */
    KafkaStreamsApplication getStreamsApplicationById(final String applicationId);

    /**
     * Creates and starts a new streams job for the specified topology into the specified environment.
     *
     * @param topologyType    the topology type.
     * @param topologyVersion the topology topologyVersion.
     * @param env             the environment name.
     * @param executed        the {@link Executed} instance.
     * @return the streams application.id
     */
    ApplicationId startStreamsTopology(final String topologyType,
                                       final String topologyVersion,
                                       final String env,
                                       final Executed executed);

    /**
     * Gets all topologies available locally.
     *
     * @return a set of {@link TopologyDescriptor} instance.
     */
    Set<TopologyDescriptor> getTopologyProviders();

    /**
     * Gets the list of all topologies.
     *
     * @return the list {@link TopologyAndAliases}.
     */
    List<TopologyAndAliases> getAllTopologies();

    /**
     * Gets the {@link TopologyDescriptor} for the specified alias and version.
     *
     * @param alias   the topology alias.
     * @param version the topology version.
     * @return the {@link TopologyDescriptor}.
     * @throws NoSuchComponentException if no topology exist for the given parameters.
     * @throws IllegalArgumentException if the component for the given parameters is not a Topology.
     */
    TopologyDescriptor getTopologyByAliasAndVersion(final String alias, final String version);

    /**
     * Gets the {@link TopologyDescriptor} for the specified alias and qualifier.
     *
     * @param alias     the topology alias.
     * @param qualifier the topology qualifier.
     * @return the {@link TopologyDescriptor}.
     * @throws NoSuchComponentException if no topology exist for the given parameters.
     * @throws IllegalArgumentException if the component for the given parameters is not a Topology.
     */
    TopologyDescriptor getTopologyByAliasAndQualifiers(final String alias,
                                                       final Qualifier<? extends TopologyProvider> qualifier);

    /**
     * Gets all versions of {@link TopologyDescriptor} for the specified alias.
     *
     * @param alias the topology alias.
     * @throws NoSuchComponentException if no topology exist for the given parameters.
     */
    List<Version> getTopologyVersionsByAlias(final String alias);

    /**
     * Gets the configuration of {@link AzkarraContext}.
     *
     * @return a {@link Conf} object.
     */
    Conf getContextConfig();

    /**
     * Gets all existing streams environments.
     *
     * @return a set of {@link StreamsExecutionEnvironment} instance.
     */
    Set<StreamsExecutionEnvironment.View> describeAllEnvironments();

    /**
     * Gets the {@link StreamsExecutionEnvironment} for the specified name.
     *
     * @param name the environment name to find.
     * @return the {@link StreamsExecutionEnvironment}.
     * @throws NotFoundException if no environment exists for the given {@code name}.
     */
    StreamsExecutionEnvironment.View describeEnvironmentByName(final String name);

    /**
     * Gets all supported environment types.
     *
     * @return the set of the environment types.
     */
    Set<String> getSupportedEnvironmentTypes();

    /**
     * Adds a new environment to this application.
     *
     * @param name the environment name.
     * @param type the environment type.
     * @param conf the environment configuration.
     */
    void addNewEnvironment(final String name, final String type, final Conf conf);

    /**
     * Stops the streams instance for the specified container id.
     *
     * @param containerId the container id.
     * @param cleanUp     the flag to indicate if the local streams states should be cleaned up.
     * @throws NotFoundException if no container exists for the id.
     * @see StreamsExecutionEnvironment#stop(ContainerId, boolean)
     */
    void stopStreamsContainer(final String containerId, final boolean cleanUp);

    /**
     * Restarts the streams instance for the specified container id.
     *
     * @param containerId the container id.
     * @throws NotFoundException if no Kafka Streams instance exists for the id.
     */
    void restartStreamsContainer(final String containerId);

    /**
     * Deletes the streams instance for the specified container id.
     *
     * @param containerId the container id.
     * @throws NotFoundException if no Kafka Streams instance exists for the id.
     * @see StreamsExecutionEnvironment#terminate(ApplicationId)
     */
    void terminateStreamsContainer(final String containerId);

    /**
     * Deletes all streams instances for the specified streams application.
     *
     * @param applicationId the streams application id.
     * @throws NotFoundException if not application exists for the id.
     * @see StreamsExecutionEnvironment#terminate(ApplicationId)
     */
    void terminateStreamsApplication(final String applicationId);
}

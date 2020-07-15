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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.components.BaseComponentModule;
import io.streamthoughts.azkarra.api.components.Qualifier;
import io.streamthoughts.azkarra.api.components.Restriction;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.streams.admin.AdminClientUtils;
import io.streamthoughts.azkarra.runtime.streams.topology.TopologyUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.runtime.streams.topology.TopologyUtils.getUserDeclaredTopics;

/**
 * This {@link StreamsLifecycleInterceptor} create both topics source and sink
 * before starting the streams instance.
 *
 * Optionally, this interceptor can also be used to automatically delete all topics used by the applications
 * when it's closed.
 *
 * This interceptor is state-full and thus a new instance must be created for each topology.
 */
public class AutoCreateTopicsInterceptor
        extends BaseComponentModule
        implements StreamsLifecycleInterceptor, Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(AutoCreateTopicsInterceptor.class);

    /**
     * The default replication factor for creating topics.
     */
    private Short replicationFactor;

    /**
     * The default number of partitions for creating topics.
     */
    private Integer numPartitions;

    /**
     * Flag to indicate if topics should be automatically deleted once the streams is closed.
     */
    private Boolean deleteTopicsOnClose;

    /**
     * The default configuration for creating topics.
     */
    private Map<String, String> topicConfigs = new HashMap<>();

    /**
     * The list of new topics to create.
     */
    private Collection<NewTopic> newTopics = new ArrayList<>();

    /**
     * The list of topics actually created for the topology.
     */
    private Set<String> createdTopics = new HashSet<>();

    private AtomicBoolean topicListed = new AtomicBoolean(false);

    private final AdminClient adminClient;

    private AutoCreateTopicsInterceptorConfig interceptorConfig;

    /**
     * Creates a new {@link AutoCreateTopicsInterceptor} instance.
     */
    public AutoCreateTopicsInterceptor() {
        this(null);
    }

    /**
     * Creates a new {@link AutoCreateTopicsInterceptor} instance.
     *
     * @param adminClient   the {@link AdminClient} to be used.
     */
    public AutoCreateTopicsInterceptor(final AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        super.configure(configuration);
        interceptorConfig = new AutoCreateTopicsInterceptorConfig(configuration);

        if (deleteTopicsOnClose == null)
            setDeleteTopicsOnStreamsClosed(interceptorConfig.isAutoDeleteTopicsEnable());

        if (numPartitions == null)
            setNumPartitions(interceptorConfig.getAutoCreateTopicsNumPartition());

        if (replicationFactor == null)
            setReplicationFactor(interceptorConfig.getAutoCreateTopicsReplicationFactor());

        Map<String, String> autoCreateTopicsConfigs = interceptorConfig.getAutoCreateTopicsConfigs();
        autoCreateTopicsConfigs.putAll(topicConfigs);
        topicConfigs = autoCreateTopicsConfigs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifecycleContext context,
                        final StreamsLifecycleChain chain) {
        checkState();
        mayInitializeNewTopicsFromContext(context.topologyName());

        if (context.streamsState() == State.Standards.CREATED) {

            final Set<String> userDeclaredTopics = getUserDeclaredTopics(context.topologyDescription());

            List<String> topicNames = getTopicNames();

            userDeclaredTopics.stream()
                .filter(Predicate.not(topicNames::contains))
                .forEach(name -> {
                    var topic = new NewTopic(name, numPartitions, replicationFactor).configs(topicConfigs);
                    newTopics.add(topic);
                });

            // Only create source, sink and intermediate topics.
            apply(this::createTopics, context);
            newTopics.forEach(t -> createdTopics.add(t.name()));
        }
        chain.execute();

        // List all topics (i.e user-topics, change-log, repartition) only if streams application is running.
        if (context.streamsState() == State.Standards.RUNNING) {
            apply(this::listTopics, context);
        }
    }

    private void mayInitializeNewTopicsFromContext(final String topologyName) {
        if (getComponentFactory() != null) {
            if (newTopics.isEmpty()) {
                final Qualifier<NewTopic> qualifier = Qualifiers.byRestriction(Restriction.streams(topologyName));
                newTopics = getAllComponents(NewTopic.class, qualifier);
            }
        } else {
            LOG.debug("Unable to discover topics to be created; no component factory configured");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(final StreamsLifecycleContext context,
                       final StreamsLifecycleChain chain) {
        chain.execute();
        if (deleteTopicsOnClose) {
            apply(this::deleteTopics, context);
        }
    }

    private void apply(final BiConsumer<AdminClient, StreamsLifecycleContext> consumer,
                       final StreamsLifecycleContext context) {
        // use a one-shot AdminClient if no one is provided.
        if (adminClient == null) {
            try (final AdminClient client = AdminClientUtils.newAdminClient(context.streamsConfig())) {
                consumer.accept(client, context);
            }
        } else {
            consumer.accept(adminClient, context);
        }
    }

    private void listTopics(final AdminClient client, final StreamsLifecycleContext context) {
        try {
            LOG.info("Listing all topics created by the streams application: {}", context.applicationId());
            CompletableFuture<Collection<TopicListing>> future = AdminClientUtils.listTopics(client);
            try {
                final Collection<TopicListing> topics = future.get();
                createdTopics.addAll(topics.stream()
                    .map(TopicListing::name)
                    .filter(name -> TopologyUtils.isInternalTopic(context.applicationId(), name))
                    .collect(Collectors.toSet())
                );
                topicListed.set(true);
            } catch (Exception e) {
                LOG.warn("Failed to list topics", e);
            }

        } catch (Exception e) {
            LOG.warn("Failed to list topics", e);
        }
    }

    private void deleteTopics(final AdminClient client, final StreamsLifecycleContext context) {
        try {
            if (!topicListed.get()) {
                listTopics(client, context);
            }
            LOG.info("Deleting topology topic(s): {}", createdTopics);
            client.deleteTopics(createdTopics).all().get();
            LOG.info("Topics deleted successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // ignore and attempts to start anyway;
        } catch (Exception e) {
            LOG.error("Failed to auto delete topics", e);
        }
    }

    private void createTopics(final AdminClient client, final StreamsLifecycleContext context) {
        LOG.info("Creating topology topic(s): {}", getTopicNames());
        try {
            client.createTopics(newTopics).all().get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause != null & cause instanceof TopicExistsException) {
                LOG.error("Cannot auto create topics - topics already exists. Error can be ignored.");
            } else {
                LOG.error("Cannot auto create topics", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // ignore and attempts to start anyway;
        }
    }

    private List<String> getTopicNames() {
        return newTopics.stream()
            .map(NewTopic::name)
            .collect(Collectors.toList());
    }

    private void checkState() {
        if (this.interceptorConfig == null) {
            throw new IllegalStateException(
                AutoCreateTopicsInterceptor.class.getName() + " must be configured before being started");
        }
    }

    /**
     * Sets the default replication factor uses for creating topics.
     *
     * @param replicationFactor the replication factor.
     */
    public void setReplicationFactor(final short replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    /**
     * Sets the default number of partitions uses for creating topics.
     * @param numPartitions the number of partitions.
     */
    public void setNumPartitions(final int numPartitions) {
        this.numPartitions = numPartitions;
    }

    /**
     * Sets the list of new topics to create.
     *
     * @param topics    the list of {@link NewTopic} instances.
     */
    public void setTopics(final Collection<NewTopic> topics) {
        this.newTopics = topics;
    }

    public void setTopicConfigs(final Map<String, String> topicConfigs) {
        this.topicConfigs = topicConfigs;
    }

    /**
     * Sets if topics should be automatically deleted once the streams is closed.
     *
     * @param delete    {@code true} to enable, otherwise {@code false}.
     */
    public void setDeleteTopicsOnStreamsClosed(final boolean delete) {
        this.deleteTopicsOnClose = delete;
    }
}

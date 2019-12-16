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

import io.streamthoughts.azkarra.api.StreamsLifeCycleChain;
import io.streamthoughts.azkarra.api.StreamsLifeCycleContext;
import io.streamthoughts.azkarra.api.StreamsLifeCycleInterceptor;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.streams.admin.AdminClientUtils;
import io.streamthoughts.azkarra.runtime.streams.topology.TopologyUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
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
 * This {@link StreamsLifeCycleInterceptor} create both topics source and sink
 * before starting the streams instance.
 *
 * Optionally, this interceptor can also be used to automatically delete all topics used by the applications
 * when it's closed.
 *
 * This interceptor is state-full and thus a new instance must be created for each topology.
 */
public class AutoCreateTopicsInterceptor implements StreamsLifeCycleInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(AutoCreateTopicsInterceptor.class);

    /**
     * The default replication factor for creating topics.
     */
    private short replicationFactor = 1;

    /**
     * The default number of partitions for creating topics.
     */
    private int numPartitions = 1;

    /**
     * Flag to indicate if topics should be automatically deleted once the streams is closed.
     */
    private boolean delete = false;

    /**
     * The default configuration for creating topics.
     */
    private Map<String, String> configs = new HashMap<>();

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
    public void onStart(final StreamsLifeCycleContext context,
                        final StreamsLifeCycleChain chain) {
        if (context.getState() == State.CREATED) {

            final Set<String> userDeclaredTopics = getUserDeclaredTopics(context.getTopology());

            List<String> topicNames = getTopicNames();

            userDeclaredTopics.stream()
                .filter(Predicate.not(topicNames::contains))
                .forEach(name -> newTopics.add(new NewTopic(name, numPartitions, replicationFactor).configs(configs)));

            // Only create source, sink and intermediate topics.
            apply(this::createTopics, context);
            newTopics.forEach(t -> createdTopics.add(t.name()));
        }
        chain.execute();

        // List all topics (i.e user-topics, change-log, repartition) only if streams application is running.
        if (context.getState() == State.RUNNING) {
            apply(this::listTopics, context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(final StreamsLifeCycleContext context,
                       final StreamsLifeCycleChain chain) {
        chain.execute();
        if (delete) {
            apply(this::deleteTopics, context);
        }
    }

    private void apply(final BiConsumer<AdminClient, StreamsLifeCycleContext> consumer,
                       final StreamsLifeCycleContext context) {
        // use a one-shot AdminClient if no one is provided.
        if (adminClient == null) {
            try (final AdminClient client = AdminClientUtils.newAdminClient(context.getStreamConfig())) {
                consumer.accept(client, context);
            }
        } else {
            consumer.accept(adminClient, context);
        }
    }

    private void listTopics(final AdminClient client, final StreamsLifeCycleContext context) {
        try {
            LOG.info("Listing all topics created by the streams application: {}", context.getApplicationId());
            CompletableFuture<Collection<TopicListing>> future = AdminClientUtils.listTopics(client);
            try {
                final Collection<TopicListing> topics = future.get();
                createdTopics.addAll(topics.stream()
                    .map(TopicListing::name)
                    .filter(name -> TopologyUtils.isInternalTopic(context.getApplicationId(), name))
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

    private void deleteTopics(final AdminClient client, final StreamsLifeCycleContext context) {
        try {
            if (!topicListed.get()) {
                listTopics(client, context);
            }
            LOG.info("Deleting topology topic(s): {}", createdTopics);
            client.deleteTopics(createdTopics).all().get();
            LOG.info("Topics deleted successfully");
        } catch (ExecutionException e) {
            LOG.error("Failed to delete topics", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // ignore and attempts to start anyway;
        }
    }

    private void createTopics(final AdminClient client, final StreamsLifeCycleContext context) {
        LOG.info("Creating topology topic(s): {}", getTopicNames());
        try {
            client.createTopics(newTopics).all().get();
        } catch (ExecutionException e) {
            LOG.error("Failed to create topics", e);
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

    public void setConfigs(final Map<String, String> configs) {
        this.configs = configs;
    }

    /**
     * Sets if topics should be automatically deleted once the streams is closed.
     *
     * @param delete    {@code true} to enable, otherwise {@code false}.
     */
    public void setDeleteTopicsOnStreamsClosed(final boolean delete) {
        this.delete = delete;
    }
}

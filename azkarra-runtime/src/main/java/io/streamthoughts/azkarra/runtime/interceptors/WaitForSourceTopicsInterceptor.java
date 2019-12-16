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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.runtime.streams.topology.TopologyUtils.getSourceTopics;

/**
 * This {@link StreamsLifeCycleInterceptor} waits for source topics to be created
 * before starting the streams instance.
 *
 * Kafka Streams fails if one of the source topic is missing (error: INCOMPLETE_SOURCE_TOPIC_METADATA);
 */
public class WaitForSourceTopicsInterceptor implements StreamsLifeCycleInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(WaitForSourceTopicsInterceptor.class);

    private final AdminClient adminClient;

    /**
     * Creates a new {@link WaitForSourceTopicsInterceptor} instance.
     */
    public WaitForSourceTopicsInterceptor() {
        this(null);
    }

    /**
     * Creates a new {@link WaitForSourceTopicsInterceptor} instance.
     *
     * @param adminClient   the {@link AdminClient} to be used.
     */
    public WaitForSourceTopicsInterceptor(final AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifeCycleContext context, final StreamsLifeCycleChain chain) {
        if (context.getState() == State.CREATED) {

            final Set<String> sourceTopics = getSourceTopics(context.getTopology())
                .stream()
                .filter(Predicate.not(TopologyUtils::isInternalTopic))
                .collect(Collectors.toSet());

            if (!sourceTopics.isEmpty()) {
                context.setState(State.WAITING_FOR_TOPICS);
                apply(context, client -> {
                    LOG.info("Waiting for source topic(s) to be created: {}", sourceTopics);
                    try {
                        AdminClientUtils.waitForTopicToExist(client, sourceTopics);
                    } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // ignore and attempts to start anyway;
                    }
                });
            }
        }
        chain.execute();
    }

    private void apply(final StreamsLifeCycleContext context, final Consumer<AdminClient> consumer) {
        // use a one-shot AdminClient if no one is provided.
        if (adminClient == null) {
            try (final AdminClient client = AdminClientUtils.newAdminClient(context.getStreamConfig())) {
                consumer.accept(client);
            }
        } else {
            consumer.accept(adminClient);
        }
    }
}

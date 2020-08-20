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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.streams.admin.AdminClientUtils;
import io.streamthoughts.azkarra.runtime.streams.topology.TopologyUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.runtime.streams.topology.TopologyUtils.getSourceTopics;

/**
 * This {@link StreamsLifecycleInterceptor} waits for source topics to be created
 * before starting the streams instance.
 *
 * Kafka Streams fails if one of the source topic is missing (error: INCOMPLETE_SOURCE_TOPIC_METADATA);
 */
public class WaitForSourceTopicsInterceptor implements StreamsLifecycleInterceptor, Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(WaitForSourceTopicsInterceptor.class);

    private final AdminClient adminClient;

    private WaitForSourceTopicsInterceptorConfig config;

    enum InterceptorState implements State { WAITING_FOR_TOPICS }

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
    public void configure(final Conf configuration) {
        this.config = new WaitForSourceTopicsInterceptorConfig(configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifecycleContext context, final StreamsLifecycleChain chain) {
        if (context.streamsState() == State.Standards.CREATED) {

            final List<Pattern> excludePatterns = config.getExcludePatterns();

            final Set<String> sourceTopics = getSourceTopics(context.topologyDescription())
                .stream()
                .filter(Predicate.not(TopologyUtils::isInternalTopic))
                .filter(topic -> excludePatterns.stream().noneMatch(it -> it.matcher(topic).matches()))
                .collect(Collectors.toSet());

            if (!sourceTopics.isEmpty()) {
                context.setState(InterceptorState.WAITING_FOR_TOPICS);
                apply(context, client -> {
                    LOG.info(
                        "Waiting for source topic(s) to be created: {} (timeout={}ms)",
                        sourceTopics,
                        config.getTimeout().toMillis()
                    );
                    try {
                        AdminClientUtils.waitForTopicToExist(client, sourceTopics, config.getTimeout());
                        LOG.info("All source topics are created. KafkaStreams instance can be safely started");
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        // ignore and attempts to start anyway;
                    } catch (TimeoutException e) {
                        LOG.warn(e.getMessage());
                    }
                });
            }
        }
        chain.execute();
    }

    private void apply(final StreamsLifecycleContext context, final Consumer<AdminClient> consumer) {
        // use a one-shot AdminClient if no one is provided.
        if (adminClient == null) {
            try (final AdminClient client = AdminClientUtils.newAdminClient(context.streamsConfig())) {
                consumer.accept(client);
            }
        } else {
            consumer.accept(adminClient);
        }
    }
}
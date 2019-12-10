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
package io.streamthoughts.azkarra.runtime.streams;

import io.streamthoughts.azkarra.api.StreamsLifeCycleChain;
import io.streamthoughts.azkarra.api.StreamsLifeCycleContext;
import io.streamthoughts.azkarra.api.StreamsLifeCycleInterceptor;
import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.streams.admin.AdminClientUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.streams.TopologyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This {@link StreamsLifeCycleInterceptor} waits for source topics to be created
 * before starting the streams instance.
 *
 * Kafka Streams fails if one of the source topic is missing (error: INCOMPLETE_SOURCE_TOPIC_METADATA);
 */
public class WaitForSourceTopicsInterceptor implements StreamsLifeCycleInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(WaitForSourceTopicsInterceptor.class);

    private static final String INTERNAL_REPARTITIONING_TOPIC_SUFFIX = "-repartition";

    /**
     * Pattern for identifying internal topics created for repartitioning purpose.
     */
    private static final Pattern INTERNAL_REPARTITIONING_NAME_PATTERN = Pattern.compile(".*-[0-9]{10}-repartition$");

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifeCycleContext context, final StreamsLifeCycleChain chain) {
        if (context.getState() == State.CREATED) {
            final Set<String> sourceTopics = getSourceTopics(context.getApplicationId(), context.getTopology());
            if (!sourceTopics.isEmpty()) {
                context.setState(State.WAITING_FOR_TOPICS);
                try (final AdminClient client = AdminClientUtils.newAdminClient(context.getStreamConfig())) {
                    LOG.info("Waiting for source topic(s) to be created: {}", sourceTopics);
                    AdminClientUtils.waitForTopicToExist(client, sourceTopics);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // ignore and attempts to start anyway;
                }
            }
        }
        chain.execute();
    }

    @VisibleForTesting
    Set<String> getSourceTopics(final String applicationId, final TopologyDescription topology) {
        final Set<String> userTopics = new HashSet<>();
        topology.globalStores().forEach(s -> {
            userTopics.addAll(s.source().topicSet());
        });
        topology.subtopologies().forEach(sub ->
            sub.nodes().forEach(n -> {
                if (n instanceof TopologyDescription.Source) {
                    Set<String> topics = ((TopologyDescription.Source) n).topicSet();
                    userTopics.addAll(
                        topics.stream()
                            .filter(Predicate.not(topic -> isInternalTopics(applicationId, topic)))
                            .collect(Collectors.toSet())
                    );
                }
            })
        );
        return userTopics;
    }

    private static boolean isInternalTopics(final String applicationId, final String topic) {
        return topic.startsWith(applicationId)
                || INTERNAL_REPARTITIONING_NAME_PATTERN.matcher(topic).matches()
                || topic.endsWith(INTERNAL_REPARTITIONING_TOPIC_SUFFIX);
    }
}

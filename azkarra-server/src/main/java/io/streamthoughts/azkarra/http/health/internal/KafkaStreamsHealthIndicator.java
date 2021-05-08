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
package io.streamthoughts.azkarra.http.health.internal;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.http.health.Health;
import io.streamthoughts.azkarra.http.health.HealthAggregator;
import io.streamthoughts.azkarra.http.health.HealthIndicator;
import io.streamthoughts.azkarra.http.health.Status;
import io.streamthoughts.azkarra.http.health.StatusAggregator;
import org.apache.kafka.streams.processor.TaskMetadata;
import org.apache.kafka.streams.processor.ThreadMetadata;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KafkaStreamsHealthIndicator implements HealthIndicator, AzkarraContextAware {

    private static final String NAME = "kafkaStreams";

    private AzkarraStreamsService service;

    private final StatusAggregator statusAggregator;

    /**
     * Creates a new {@link KafkaStreamsHealthIndicator} instance.
     */
    public KafkaStreamsHealthIndicator() {
        this.statusAggregator = new DefaultStatusAggregator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Health getHealth() {
        final List<Health> healths = getAllStreams()
                .stream()
                .map(it -> new Health
                        .Builder()
                        .withName(it.applicationId())
                        .withStatus(buildStatus(it))
                        .withDetails(buildDetails(it))
                        .build()
                )
                .sorted(Comparator.comparing(Health::getName))
                .collect(Collectors.toList());
        return new HealthAggregator(statusAggregator).aggregate(NAME, healths);
    }

    /**
     * Builds up status for the stream.
     *
     * @return The {@link Status}.
     */
    private Status buildStatus(final KafkaStreamsContainer stream) {
        final State value = stream.state().value();
        if (value.equals(State.Standards.RUNNING)) {
            // return DOWN if one StreamThread is not RUNNING.
            return stream.threadMetadata()
                  .stream()
                  .filter(t -> !t.threadState().equals(State.Standards.RUNNING.name()))
                  .findAny()
                  .map( it -> Status.DOWN)
                  .orElse(Status.UP);
        } else if (value.equals(State.Standards.ERROR)) {
            return Status.DOWN;
        } else if (value.equals(State.Standards.NOT_RUNNING)) {
            return stream.exception().isPresent() ? Status.DOWN : Status.UNKNOWN;
        } else {
            return Status.UNKNOWN;
        }
    }

    /**
     * Builds up details for the stream.
     *
     * @return The {@link Map} of details.
     */
    private Map<String, Object> buildDetails(final KafkaStreamsContainer container) {
        final Map<String, Object> streamDetails = new HashMap<>();

        final State state = container.state().value();
        streamDetails.put("state", state);
        if (state.equals(State.Standards.RUNNING) || state.equals(State.Standards.REBALANCING)) {
            final Map<String, Object> streamThreadsDetails = new HashMap<>();
            streamDetails.put("streamThreads", streamThreadsDetails);
            for (ThreadMetadata metadata : container.threadMetadata()) {
                final HashMap<String, Object> threadDetails = new HashMap<>();
                threadDetails.put("threadState", metadata.threadState());
                threadDetails.put("adminClientId", metadata.adminClientId());
                threadDetails.put("consumerClientId", metadata.consumerClientId());
                threadDetails.put("restoreConsumerClientId", metadata.restoreConsumerClientId());
                threadDetails.put("producerClientIds", metadata.producerClientIds());
                threadDetails.put("activeTasks", taskDetails(metadata.activeTasks()));
                threadDetails.put("standbyTasks", taskDetails(metadata.standbyTasks()));
                streamThreadsDetails.put(metadata.threadName(), threadDetails);
            }
        } else {
            container.exception().ifPresent(error -> streamDetails.put("error", error));
        }
        return streamDetails;
    }

    /**
     * Gets task details for the kafka stream.
     *
     * @param allTaskMetadata the {@link TaskMetadata}.
     * @return                The {@link Map} of details.
     */
    private static Map<String, Object> taskDetails(final Set<TaskMetadata> allTaskMetadata) {
        final Map<String, Object> details = new HashMap<>();
        for (TaskMetadata taskMetadata : allTaskMetadata) {
            details.put("taskId", taskMetadata.taskId());
            if (details.containsKey("partitions")) {
                @SuppressWarnings("unchecked")
                final List<String> partitionsInfo = (List<String>) details.get("partitions");
                partitionsInfo.addAll(addPartitionsInfo(taskMetadata));
            } else {
                details.put("partitions", addPartitionsInfo(taskMetadata));
            }
        }
        return details;
    }

    /**
     * Adds partition details if available.
     *
     * @param metadata The {@link TaskMetadata}
     * @return         List of partition and topic details
     */
    private static List<String> addPartitionsInfo(final TaskMetadata metadata) {
        return metadata.topicPartitions().stream()
                .map(p -> "partition=" + p.partition() + ", topic=" + p.topic())
                .collect(Collectors.toList());
    }

    private List<KafkaStreamsContainer> getAllStreams() {
        return this.service.listAllKafkaStreamsContainerIds()
                .stream()
                .map(service::getStreamsContainerById)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAzkarraContext(final AzkarraContext context) {
        this.service = context.getComponent(AzkarraStreamsService.class);
    }
}

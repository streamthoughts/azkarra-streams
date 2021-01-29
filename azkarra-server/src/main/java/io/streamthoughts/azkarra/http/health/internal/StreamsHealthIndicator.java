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
import io.streamthoughts.azkarra.http.health.HealthIndicator;
import io.streamthoughts.azkarra.http.health.Status;
import io.streamthoughts.azkarra.http.health.StatusAggregator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class StreamsHealthIndicator implements HealthIndicator, AzkarraContextAware {

    private static final String STATE_DETAIL = "state";
    private static final String APPLICATIONS = "applications";

    private AzkarraStreamsService service;

    private StatusAggregator statusAggregator;

    /**
     * Creates a new {@link StreamsHealthIndicator} instance.
     */
    public StreamsHealthIndicator() {
        this.statusAggregator = new DefaultStatusAggregator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Health getHealth() {
        List<KafkaStreamsContainer> streams = getAllStreams();

        Map<String, Health> healths = new TreeMap<>();

        for (KafkaStreamsContainer container : streams) {
            Health.Builder builder = new Health
                .Builder()
                .withDetails(STATE_DETAIL, container.state().value());
            setHealthStatus(container, builder);
            healths.put(container.applicationId(), builder.build());
        }
        Health.Builder builder = new Health.Builder();
        healths.forEach(builder::withDetails);

        Status status = statusAggregator.aggregateStatus(StatusAggregator.getAllStatus(healths.values()));

        return builder.withName(APPLICATIONS).withStatus(status).build();
    }

    private void setHealthStatus(final KafkaStreamsContainer container,
                                 final Health.Builder builder) {
        final State value = container.state().value();
        final Optional<Throwable> exception = container.exception();

        if (value.equals(State.Standards.RUNNING)) {
            builder.up();

        } else if (value.equals(State.Standards.ERROR)) {
            builder.down();
            exception.ifPresent(builder::withException);
        }
        else if (value.equals(State.Standards.NOT_RUNNING)) {
            exception.ifPresentOrElse(e -> builder.down().withException(e), builder::unknown);

        } else {
            builder.unknown();
        }
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

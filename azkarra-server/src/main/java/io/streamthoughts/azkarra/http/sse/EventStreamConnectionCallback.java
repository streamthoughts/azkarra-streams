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
package io.streamthoughts.azkarra.http.sse;

import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.errors.NotFoundException;
import io.streamthoughts.azkarra.api.events.reactive.EventStreamPublisher;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.serialization.json.Json;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.server.handlers.sse.ServerSentEventConnectionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 0.8.0
 */
public class EventStreamConnectionCallback implements ServerSentEventConnectionCallback {

    private static final Logger LOG = LoggerFactory.getLogger(EventStreamConnectionCallback.class);

    private final AzkarraStreamsService service;

    private final Json json;

    /**
     * Creates a new {@link EventStreamConnectionCallback} instance.
     *
     * @param service   the {@link AzkarraStreamsService} instance.
     */
    public EventStreamConnectionCallback(final AzkarraStreamsService service,
                                         final Json json) {
        this.service = service;
        this.json = json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void connected(final ServerSentEventConnection connection,
                          final String lastEventId) {
        var applicationId = connection.getParameter("id");
        var eventChannel = connection.getParameter("event");
        LOG.info(
            "ServerSentEventConnection established. Subscribe to event-stream for application='{}', type='{}'",
            applicationId,
            eventChannel
        );

        try {
            final KafkaStreamsContainer container = service.getStreamsById(applicationId);
            final EventStreamPublisher publisher = container.eventStreamPublisherForType(eventChannel);
            if (publisher == null) {
                connection.shutdown();
                return;
            }
            publisher.subscribe(new ServerSentEventSubscriber<>(connection, publisher.type(), applicationId, json));

        } catch (NotFoundException e) {
            LOG.error(e.getMessage());
            connection.shutdown();
        }
    }
}

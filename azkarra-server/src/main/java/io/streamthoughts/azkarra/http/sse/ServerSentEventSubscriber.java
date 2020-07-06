/*
 * Copyright 2020 StreamThoughts.
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

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.serialization.json.Json;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Flow;

/**
 * A subscriber that sent published {@link KV} records over HTTP using a Server-Sent Event (SSE) connection.
 *
 * @param <K>   the record-key type
 * @param <V>   the record-value type.
 *
 * @since 0.8.0
 */
public class ServerSentEventSubscriber<K, V> implements Flow.Subscriber<KV<K, V>> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerSentEventSubscriber.class);

    private final String eventType;
    private final ServerSentEventConnection connection;
    private final String applicationId;
    private Flow.Subscription subscription;
    private final Json json;

    /**
     * Creates a new {@link ServerSentEventSubscriber} instance.
     *
     * @param connection    the {@link ServerSentEventConnection}.
     * @param eventType     the event type.
     * @param applicationId the {@code application.id} of KafkaStreams, used for logging.
     * @param json          the {@link Json} serializer.
     */
    public ServerSentEventSubscriber(final ServerSentEventConnection connection,
                                     final String eventType,
                                     final String applicationId,
                                     final Json json) {
        this.connection = connection;
        this.eventType = eventType;
        this.applicationId = applicationId;
        this.json = json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSubscribe(final Flow.Subscription subscription) {
        Objects.requireNonNull(subscription); // rule 2.13
        if (this.subscription != null) {
            subscription.cancel(); // Cancel the additional subscription
        } else {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNext(final KV<K, V> record) {
        Objects.requireNonNull(record); // rule 2.13
        if (connection.isOpen()) {
            final String payload = json.serialize(StreamedEvent.record(record));
            connection.send(payload, eventType, null, new ServerSentEventConnection.EventCallback() {
                @Override
                public void done(final ServerSentEventConnection connection,
                                 final String data,
                                 final String event,
                                 final String id) {
                    LOG.debug(
                        "Event sent for application='{}', type='{}'",
                        applicationId,
                        eventType
                    );
                }

                @Override
                public void failed(final ServerSentEventConnection connection,
                                   final String data,
                                   final String event,
                                   final String id,
                                   final IOException e) {
                    LOG.debug(
                        "Failed to send event for application='{}', type='{}'",
                        applicationId,
                        eventType
                    );
                }
            });
        } else {
            subscription.cancel(); // Cancel the subscription if the connection is closed.
            LOG.info(
                "ServerSentEventConnection was closed. Canceling subscription for application='{}', type='{}'.",
                applicationId,
                eventType
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(final Throwable throwable) {
        Objects.requireNonNull(throwable); // rule 2.13
        LOG.error("Unexpected error while consuming from . Closing ServerSentEventConnection", throwable);
        closeConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onComplete() {
        LOG.info(
            "Closing ServerSentEventConnection for application='{}', type='{}'",
            applicationId,
            eventType
        );
        closeConnection();
    }

    private void closeConnection() {
        connection.shutdown();
    }

    public static class StreamedEvent {

        private final  KV<?, ?> record;

        public static StreamedEvent record(final KV<?, ?> record) {
            return new StreamedEvent(record);
        }

        public StreamedEvent(final KV<?, ?> record) {
            this.record = record;
        }

        public KV getRecord() {
            return record;
        }
    }
}


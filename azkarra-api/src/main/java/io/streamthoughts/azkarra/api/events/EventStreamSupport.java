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
package io.streamthoughts.azkarra.api.events;

import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.model.KV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for {@link EventStreamProvider}.
 * This class provides convenient methods to register {@link EventStream} and to send records.
 *
 * @since 0.8.0
 */
public class EventStreamSupport implements EventStreamProvider {

    private final Map<String, EventStream<?, ?>> streams = new HashMap<>();

    private LimitHandler defaultLimitHandler;

    private int defaultQueueSize;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EventStream> getEventStreams() {
        return new ArrayList<>(streams.values());
    }

    public <K, V> void addEventStream(final EventStream<K, V> eventStream) {
        if (streams.put(eventStream.type(), eventStream) != null) {
            throw new IllegalArgumentException("EventStream already registered for type: " + eventStream.type());
        }
    }

    protected void addEventStreamsWithDefaults(final String... types) {
        for (final String type : types) {
            addEventStream(new EventStream.Builder(type)
                .withQueueSize(defaultQueueSize)
                .withQueueLimitHandler(defaultLimitHandler)
                .build()
            );
        }
    }

    /**
     * Sets the default {@link LimitHandler} that will be used for creating {@link EventStream}.
     *
     * @param limitHandler  the {@link LimitHandler}.
     */
    protected void setDefaultEventQueueLimitHandler(final LimitHandler limitHandler) {
        defaultLimitHandler = Objects.requireNonNull(limitHandler, "limitHandler cannot be null");
    }

    /**
     * Sets the default event queue size that will be used for creating {@link EventStream}.
     *
     * @param queueSize  the queue size.
     */
    protected void setDefaultEventQueueSize(final int queueSize) {
        defaultQueueSize = queueSize;
    }

    /**
     * Sends a key-value record into this queue.
     *
     * @param type  the type of the event-stream.
     * @param value the record value.
     */
    public <K, V>  void send(final String type, final V value) {
        getEventStreamOrThrow(type).send(KV.of(null, value));
    }

    /**
     * Sends a key-value record into the given stream.
     *
     * @param type  the type of the event-stream.
     * @param key   the record key.
     * @param value the record value.
     */
    public <K, V> void send(final String type, final K key, final V value) {
        getEventStreamOrThrow(type).send(KV.of(key, value));
    }

    /**
     * Sends a timestamped key-value record into the given stream.
     *
     * @param type  the type of the event-stream.
     * @param key   the record key.
     * @param value the record value.
     */
    public <K, V>  void send(final String type, final K key, final V value, final long timestamp) {
        getEventStreamOrThrow(type).send(KV.of(key, value, timestamp));
    }

    /**
     * Sends a null-key value record into into the given stream.
     *
     * @param type  the type of the event-stream.
     * @param kv    the {@link KV} record.
     */
    public <K, V>  void send(final String type, final KV<K, V> kv) {
        getEventStreamOrThrow(type).send(kv);
    }

    @SuppressWarnings("unchecked")
    private <K, V> EventStream<K, V> getEventStreamOrThrow(final String type) {
        EventStream<?, ?> es = streams.get(type);
        if (es == null) {
            throw new AzkarraException("No EventStream registered for type: " + type);
        }
        return (EventStream<K, V>)es;
    }
}

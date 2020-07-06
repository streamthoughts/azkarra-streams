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

import io.streamthoughts.azkarra.api.model.KV;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A typed stream of events backed by a {@link BlockingRecordQueue}.
 *
 * @param <K>   the record key type.
 * @param <V>   the record value type.
 *
 * @since 0.8.0
 */
public class EventStream<K, V> {

    private final String type;
    private final BlockingRecordQueue<K, V> queue;

    private final AtomicBoolean opened = new AtomicBoolean(false);

    public static class Builder {

        private String eventType;
        private Integer queueSize;
        private LimitHandler queueLimitHandler;

        /**
         * Creates a new {@link Builder} instance.
         *
         * @param eventType the event-type.
         */
        public Builder(final String eventType) {
            this.eventType = eventType;
        }

        public Builder withQueueSize(final int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public Builder withQueueLimitHandler(final LimitHandler queueLimitHandler) {
            this.queueLimitHandler = queueLimitHandler;
            return this;
        }

        public <K, V> EventStream<K, V> build() {
            var queue = queueSize != null ?
                new BasicBlockingRecordQueue<K, V>(queueSize) :
                new BasicBlockingRecordQueue<K, V>();

            if (queueLimitHandler != null)
                queue.setLimitHandler(queueLimitHandler);
            return new EventStream<>(eventType, queue);
        }
    }

    /**
     * Creates a new {@link EventStream} instance.
     *
     * @param type  the name of the events send to this stream.
     * @param queue the {@link BlockingRecordQueue}.
     */
    public EventStream(final String type, final BlockingRecordQueue<K, V> queue) {
        this.type = Objects.requireNonNull(type, "eventType cannot be null");
        this.queue = Objects.requireNonNull(queue, "queue cannot be null");
    }

    /**
     * @return  the name of the events send to this stream.
     */
    public String type() {
        return type;
    }

    public synchronized void open(final EventStreamPipe<K, V> pipe) {
        Objects.requireNonNull(pipe, "pipe cannot be null");
        if (opened.get()) {
           throw new IllegalStateException("This stream is already open");
        }
        pipe.onOpen(queue);
        queue.open();
    }

    public synchronized void close() {
        try {
            queue.close();
            queue.clear();
        } finally {
            opened.set(false);
        }
    }

    /**
     * Sends a key-value record into this queue.
     *
     * @param value    the record value.
     */
    public void send(final V value) {
        send(KV.of(null, value));
    }

    /**
     * Sends a key-value record into this stream.
     *
     * @param key      the record key.
     * @param value    the record value.
     */
    public void send(final K key, final V value) {
        send(KV.of(key, value));
    }

    /**
     * Sends a timestamped key-value record into this stream.
     *
     * @param key      the record key.
     * @param value    the record value.
     */
    public void send(final K key, final V value, final long timestamp) {
        send(KV.of(key, value, timestamp));
    }

    /**
     * Sends a null-key value record into this stream.
     *
     * @param kv    the {@link KV} record.
     */
    public void send(final KV<K, V> kv) {
        queue.send(kv);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "EventStream{" + "type='" + type + "\"}";
    }
}

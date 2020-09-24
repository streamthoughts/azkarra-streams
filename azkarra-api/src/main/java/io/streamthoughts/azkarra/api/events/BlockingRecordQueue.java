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
package io.streamthoughts.azkarra.api.events;

import io.streamthoughts.azkarra.api.events.callback.QueueCallback;
import io.streamthoughts.azkarra.api.model.KV;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A blocking queue that can be used for publishing key-value records from a Kafka Streams topology.
 *
 * @param <K> the record-key type
 * @param <V> the record-value type
 *
 * @since 0.8.0
 */
public interface BlockingRecordQueue<K, V> {

    /**
     * Sets the handler to be invoked when the limit queue is reached.
     *
     * @param handler   the {@link LimitHandler} instance.
     */
    void setLimitHandler(final LimitHandler handler);

    /**
     * Sets the callback to be executed when a new record is queued or the queue is closed.
     *
     * @param callback  the {@link QueueCallback} to execute.
     */
    void setQueueCallback(final QueueCallback callback);

    /**
     * @see BlockingQueue#poll(long, TimeUnit)
     */
    KV<K, V> poll(final Duration timeout) throws InterruptedException;

    /**
     * @see BlockingQueue#poll()
     */
    KV<K, V> poll();

    /**
     * @see BlockingQueue#drainTo(Collection)
     */
    void drainTo(final Collection<? super KV<K, V>> collection);

    /**
     * @return  the number of records queued.
     */
    int size();

    /**
     *
     * @return {@code true} if the queue is empty.
     */
    boolean isEmpty();

    /**
     * Open the queue.
     */
    void open();

    /**
     * Close the queue. The records sent after the queue is closed will be ignored.
     */
    void close();

    /**
     * Sends a key-value record into this queue.
     *
     * @param kv    the {@link KV} record.
     */
    void send(final KV<K, V> kv);

    /**
     * @see BlockingQueue#clear()
     */
    void clear();
}

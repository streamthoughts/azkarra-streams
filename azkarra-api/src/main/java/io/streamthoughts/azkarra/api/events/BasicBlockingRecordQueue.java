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

import io.streamthoughts.azkarra.api.events.callback.LimitQueueCallback;
import io.streamthoughts.azkarra.api.events.callback.LimitedQueueCallback;
import io.streamthoughts.azkarra.api.events.callback.QueueCallback;
import io.streamthoughts.azkarra.api.model.KV;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple {@link BlockingRecordQueue} backed by a {@link BlockingQueue}.
 *
 * @param <K>     the record-key type.
 * @param <V>     the record-value type.
 *
 * @since 0.8.0
 */
public class BasicBlockingRecordQueue<K, V> implements BlockingRecordQueue<K, V> {

    static final int DEFAULT_QUEUE_SIZE_LIMIT = 10_000;

    static final Duration DEFAULT_MAX_BLOCK_DURATION = Duration.ofMillis(100);

    private final BlockingQueue<KV<K, V>> blockingQueue;

    private final Duration maxBlockDuration;

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private LimitQueueCallback callback;

    /**
     * Creates a new {@link BasicBlockingRecordQueue} instance.
     */
    public BasicBlockingRecordQueue() {
        this(DEFAULT_QUEUE_SIZE_LIMIT);
    }

    /**
     * Creates a new {@link BasicBlockingRecordQueue} instance.
     *
     * @param queueSizeLimit    the queue max capacity.
     */
    public BasicBlockingRecordQueue(final int queueSizeLimit) {
        this(queueSizeLimit, DEFAULT_MAX_BLOCK_DURATION);
    }

    /**
     * Creates a new {@link BasicBlockingRecordQueue} instance.
     *
     * @param queueSizeLimit    the queue max capacity.
     * @param maxBlockDuration  the maximum duration to wait to wait before giving up,  when the queue is full.
     */
    public BasicBlockingRecordQueue(final int queueSizeLimit, final Duration maxBlockDuration) {
        this(queueSizeLimit, maxBlockDuration, LimitHandlers.NO_OP);
    }


    /**
     * Creates a new {@link BasicBlockingRecordQueue} instance.
     *
     * @param queueSizeLimit    the blocking size limit;
     * @param maxBlockDuration  the maximum duration to wait to wait before giving up,  when the queue is full.
     * @param limitHandler      the {@link LimitHandler} to invoke after {@code maxBlockDuration} has been reached.
     */
    public BasicBlockingRecordQueue(final int queueSizeLimit,
                                    final Duration maxBlockDuration,
                                    final LimitHandler limitHandler) {
        if (queueSizeLimit <= 0)
            throw new IllegalArgumentException("queueSizeLimit must be superior to 0, was :" + queueSizeLimit);

        this.blockingQueue = new LinkedBlockingQueue<>(queueSizeLimit);
        this.maxBlockDuration = maxBlockDuration;
        this.callback = new LimitedQueueCallback(queueSizeLimit);
        this.callback.setQueue(this);
        this.callback.setLimitHandler(limitHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLimitHandler(final LimitHandler limitHandler) {
        Objects.requireNonNull(limitHandler, "limitHandler cannot be null");
        this.callback.setLimitHandler(limitHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setQueueCallback(final QueueCallback callback) {
        Objects.requireNonNull(callback, "callback cannot be null");
        var parent = this.callback;
        this.callback = new LimitQueueCallback() {
            @Override
            public void setLimitHandler(final LimitHandler limitHandler) {
                parent.setLimitHandler(limitHandler);
            }

            @Override
            public void setQueue(final BlockingRecordQueue queue) {
                parent.setQueue(queue);
            }

            @Override
            public void onQueued() {
                callback.onQueued();
                parent.onQueued();
            }

            @Override
            public void onClosed() {
                callback.onClosed();
                parent.onClosed();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KV<K, V> poll(final Duration timeout) throws InterruptedException {
        return blockingQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KV<K, V> poll() {
        return blockingQueue.poll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drainTo(final Collection<? super KV<K, V>> collection) {
        blockingQueue.drainTo(collection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return blockingQueue.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return blockingQueue.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {
        isClosed.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        isClosed.set(true);
        if (callback != null) callback.onClosed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final KV<K, V> record) {
        if (record == null)
            return;

        try {
            while (!isClosed.get()) {
                if (blockingQueue.offer(record, maxBlockDuration.toMillis(), TimeUnit.MILLISECONDS)) {
                    if (callback != null) callback.onQueued();
                    break;
                }
            }
        } catch (final InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        this.blockingQueue.clear();
    }
}

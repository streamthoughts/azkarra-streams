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
package io.streamthoughts.azkarra.api.events.callback;

import io.streamthoughts.azkarra.api.events.BlockingRecordQueue;
import io.streamthoughts.azkarra.api.events.LimitHandler;
import io.streamthoughts.azkarra.api.events.LimitHandlers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default {@link LimitQueueCallback} implementation.
 *
 * @since 0.8.0
 */
public class LimitedQueueCallback implements LimitQueueCallback {

    private final AtomicInteger remaining;
    private volatile LimitHandler limitHandler = LimitHandlers.NO_OP;
    private volatile BlockingRecordQueue<?, ?> queue;
    private final int limit;

    /**
     * Creates a new {@link LimitedQueueCallback} instance.
     *
     * @param limit  the queue limit.
     */
    public LimitedQueueCallback(final int limit) {
        if (limit < 0) throw new IllegalArgumentException("limit must be positive, given: " + limit);
        this.limit = limit;
        this.remaining = new AtomicInteger(limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLimitHandler(final LimitHandler limitHandler) {
        this.limitHandler = Objects.requireNonNull(limitHandler, "limitHandler");
        if (remaining.get() == 0) {
            limitHandler.onLimitReached(queue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setQueue(final BlockingRecordQueue queue) {
        this.queue = queue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onQueued() {
        if (remaining.decrementAndGet() == 0) {
            limitHandler.onLimitReached(queue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClosed() {
        // reset callback.
        this.remaining.set(limit);
    }
}
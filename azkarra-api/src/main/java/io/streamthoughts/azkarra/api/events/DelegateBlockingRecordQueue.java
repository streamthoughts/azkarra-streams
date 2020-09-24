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
import java.util.Objects;

/**
 *  A delegating {@link BlockingRecordQueue} that can be used to easily override some methods.
 *
 * @param <K>   the record-key type.
 * @param <V>   the record-value type.
 */
public class DelegateBlockingRecordQueue<K, V> implements BlockingRecordQueue<K, V> {

    private final BlockingRecordQueue<K, V> delegate;

    protected DelegateBlockingRecordQueue(final BlockingRecordQueue<K, V> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegating queue cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLimitHandler(final LimitHandler handler) {
        delegate.setLimitHandler(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setQueueCallback(final QueueCallback callback) {
        delegate.setQueueCallback(callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KV<K, V> poll(final Duration timeout) throws InterruptedException {
        return delegate.poll(timeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KV<K, V> poll() {
        return delegate.poll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drainTo(final Collection<? super KV<K, V>> collection) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return delegate.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {
        delegate.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        delegate.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(KV<K, V> record) {
        delegate.send(record);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        delegate.clear();
    }
}
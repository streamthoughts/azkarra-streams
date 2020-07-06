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
package io.streamthoughts.azkarra.api.events.reactive;

import io.streamthoughts.azkarra.api.events.BlockingRecordQueue;
import io.streamthoughts.azkarra.api.events.EventStream;
import io.streamthoughts.azkarra.api.events.EventStreamProvider;
import io.streamthoughts.azkarra.api.model.KV;

import java.util.concurrent.Flow;

/**
 * The main publisher interface to subscribe to a specific streams of key-value records.
 *
 * @see EventStream
 * @see EventStreamProvider
 * @see BlockingRecordQueue
 *
 * @param <K>       the record key type.
 * @param <V>       the record value type.
 *
 * @since 0.8.0
 */
public interface EventStreamPublisher<K, V> extends Flow.Publisher<KV<K, V>> {

    /**
     * Get the event type name attached to this publisher.
     *
     * @return the event type name.
     */
    String type();

    /**
     * {@inheritDoc}
     */
    @Override
    void subscribe(final Flow.Subscriber<? super KV<K, V>> subscriber);

}

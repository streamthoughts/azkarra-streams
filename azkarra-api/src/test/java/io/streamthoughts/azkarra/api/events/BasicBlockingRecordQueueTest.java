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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class BasicBlockingRecordQueueTest {

    private static final KV<Object, Object> TEST_KV = KV.of("key", "value1");

    @Test
    public void shouldInvokedLimitHandlerWhenQueueLimitIsReached() {
        // Given
        var queue = new BasicBlockingRecordQueue<>(1);
        var limitReached = new AtomicBoolean(false);
        queue.setLimitHandler(new LimitHandler() {
            @Override
            public <K, V> void onLimitReached(final BlockingRecordQueue<K, V> queue) {
                limitReached.set(true);
            }
        });
        // When
        queue.send(TEST_KV);
        // Then
        Assertions.assertTrue(limitReached.get());
    }

    @Test
    public void shouldReturnLastSendRecordOnPollGivenNonEmptyQueue() {
        // Given
        var queue = new BasicBlockingRecordQueue<>();
        queue.send(TEST_KV);
        Assertions.assertFalse(queue.isEmpty());
        // When/Then
        Assertions.assertEquals(TEST_KV, queue.poll());
    }

    @Test
    public void shouldNotAddRecordToQueueGivenNull() {
        // Given
        var queue = new BasicBlockingRecordQueue<>();
        // When
        queue.send(null);
        // Then
        Assertions.assertTrue(queue.isEmpty());
    }

    @Test
    public void shouldNotAddRecordWhenQueueIsClosed() {
        // Given
        var queue = new BasicBlockingRecordQueue<>();
        // When
        queue.close();
        // Then
        queue.send(TEST_KV);
        Assertions.assertTrue(queue.isEmpty());
    }

    @Test
    public void shouldInvokeQueueCallbackWhenQueueIsClosed() {
        // Given
        var queue = new BasicBlockingRecordQueue<>();
        var called = new AtomicBoolean(false);
        queue.setQueueCallback(new QueueCallback() {
            @Override
            public void onClosed() {
                called.set(true);
            }

            @Override
            public void onQueued() {

            }
        });

        // When
        queue.close();

        // Then
        Assertions.assertTrue(called.get());
    }

    @Test
    public void shouldInvokeQueueCallbackWhenRecordIsQueued() {
        // Given
        var queue = new BasicBlockingRecordQueue<>();
        var queued = new AtomicBoolean(false);
        queue.setQueueCallback(new QueueCallback() {
            @Override
            public void onClosed() {
            }

            @Override
            public void onQueued() {
                queued.set(true);
            }
        });
        // When
        queue.send(TEST_KV);

        // Then
        Assertions.assertTrue(queued.get());
    }
}
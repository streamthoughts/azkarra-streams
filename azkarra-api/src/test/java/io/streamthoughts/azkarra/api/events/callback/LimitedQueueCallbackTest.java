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
package io.streamthoughts.azkarra.api.events.callback;

import io.streamthoughts.azkarra.api.events.BlockingRecordQueue;
import io.streamthoughts.azkarra.api.events.LimitHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class LimitedQueueCallbackTest {

    @Test
    public void shouldInvokeLimitHandlerWhenLimitIsReached() {
        // Given
        LimitedQueueCallback callback = new LimitedQueueCallback(1);
        // When
        var limitReached = new AtomicBoolean(false);
        callback.setLimitHandler(new LimitHandler() {
            @Override
            public <K, V> void onLimitReached(final BlockingRecordQueue<K, V> queue) {
                limitReached.set(true);
            }
        });
        callback.onQueued();
        // Then
        Assertions.assertTrue(limitReached.get());
    }
}
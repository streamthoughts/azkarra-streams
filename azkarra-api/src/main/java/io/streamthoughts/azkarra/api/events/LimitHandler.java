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

import io.streamthoughts.azkarra.api.errors.AzkarraException;

/**
 * The default interface to be used for executing logic when buffer limit is reached.
 *
 * @see BlockingRecordQueue
 *
 * @since 0.8.0
 */
@FunctionalInterface
public interface LimitHandler {

    class BlockingQueueLimitReachedException extends AzkarraException {
        BlockingQueueLimitReachedException(final String message) {
            super(message);
        }
    }

    /**
     * Invokes when the limit of a {@link BlockingRecordQueue} is reached.
     *
     * @param queue the queue that reached its limit.
     */
    <K, V> void onLimitReached(final BlockingRecordQueue<K, V> queue);
}



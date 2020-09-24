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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.streamthoughts.azkarra.api.events.LimitHandler.BlockingQueueLimitReachedException;

/**
 * Provides built-in {@link LimitHandler} implementation.
 *
 * @since 0.8.0
 */
public class LimitHandlers {

    private static final Logger LOG = LoggerFactory.getLogger(DropHeadOnLimitReached.class);

    public static final LimitHandler NO_OP = new LimitHandler() {
        @Override
        public <K, V> void onLimitReached(final BlockingRecordQueue<K, V> queue) { }
    };

    /**
     * @return a new {@link LimitHandler} that log anc continue when queue limit is reached.
     */
    public static LimitHandler logAndContinueOnLimitReached() {
        return new LogAndContinueOnLimitReached();
    }

    /**
     * @return a new {@link LimitHandler} that throws a {@link BlockingQueueLimitReachedException}
     * when queue limit is reached.
     */
    public static LimitHandler throwExceptionOnLimitReached() {
        return new FailOnLimitReached();
    }

    /**
     * @return a new {@link LimitHandler} that retrieves and drop the head of the when queue limit is reached.
     */
    public static LimitHandler dropHeadOnLimitReached() {
        return new FailOnLimitReached();
    }

    private static final class LogAndContinueOnLimitReached implements LimitHandler {

        /**
         * {@inheritDoc}
         */
        @Override
        public <K, V> void onLimitReached(final BlockingRecordQueue<K, V> queue) {
            LOG.warn("Blocking queue limit reached. Ignore and continue");
        }
    }

    private static final class DropHeadOnLimitReached implements LimitHandler {

        /**
         * {@inheritDoc}
         */
        @Override
        public <K, V> void onLimitReached(final BlockingRecordQueue<K, V> queue) {
            LOG.warn("Blocking queue limit reached. Dropping head record");
            queue.poll();
        }
    }


    private static final class FailOnLimitReached implements LimitHandler {
        /**
         * {@inheritDoc}
         */
        @Override
        public <K, V> void onLimitReached(final BlockingRecordQueue<K, V> queue) {
            throw new BlockingQueueLimitReachedException("Blocking queue limit reached");
        }
    }
}

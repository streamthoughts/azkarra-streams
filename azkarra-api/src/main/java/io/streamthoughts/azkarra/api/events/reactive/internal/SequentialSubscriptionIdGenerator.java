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
package io.streamthoughts.azkarra.api.events.reactive.internal;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Class that can be used to generate a sequential {@link SubscriptionId}.
 *
 * @since 0.8.0
 */
public class SequentialSubscriptionIdGenerator implements SubscriptionIdGenerator {

    private final AtomicLong counter = new AtomicLong(0);

    /**
     * {@inheritDoc}
     */
    @Override
    public LongSubscriptionId generateNext() {
        return new LongSubscriptionId(counter.getAndIncrement());
    }
}

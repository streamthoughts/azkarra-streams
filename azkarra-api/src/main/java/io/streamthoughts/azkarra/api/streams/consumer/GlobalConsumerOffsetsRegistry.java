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
package io.streamthoughts.azkarra.api.streams.consumer;

import java.util.concurrent.ConcurrentHashMap;

public final class GlobalConsumerOffsetsRegistry implements ConsumerGroupOffsetsRegistry {

    private static final GlobalConsumerOffsetsRegistry INSTANCE = new GlobalConsumerOffsetsRegistry();

    public static GlobalConsumerOffsetsRegistry getInstance() {
        return INSTANCE;
    }

    private final ConcurrentHashMap<String, ConsumerGroupOffsetsState> consumerGroupPositions;

    /**
     * Creates a new {@link GlobalConsumerOffsetsRegistry} instance.
     */
    private GlobalConsumerOffsetsRegistry() {
        this.consumerGroupPositions = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerGroupOffsetsState offsetsFor(final String group) {
        return consumerGroupPositions.computeIfAbsent(group, ConsumerGroupOffsetsState::new);
    }
}

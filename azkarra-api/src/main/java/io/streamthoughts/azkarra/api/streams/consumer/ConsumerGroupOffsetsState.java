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

import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.monad.Tuple;
import org.apache.kafka.common.TopicPartition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ConsumerGroupOffsetsState {

    private final String group;

    private final Map<TopicPartition, Tuple<ConsumerThreadKey, ConsumerLogOffsets>> offsets;

    /**
     * Creates a new {@link ConsumerGroupOffsetsState} instance.
     *
     * @param group      the consumer group.
     */
    ConsumerGroupOffsetsState(final String group) {
        this.group = Objects.requireNonNull(group, "group cannot be null");
        this.offsets = new ConcurrentHashMap<>();
    }

    public ConsumerGroupOffsets snapshot() {
        Map<ConsumerThreadKey, ConsumerClientOffsets> offsetsGroupedByThread = new HashMap<>();
        for (Tuple<ConsumerThreadKey, ConsumerLogOffsets> tuple : offsets.values()) {
            ConsumerClientOffsets consumerClientOffsets = offsetsGroupedByThread.computeIfAbsent(
                tuple.left(),
                ConsumerClientOffsets::new
            );
            consumerClientOffsets.update(tuple.right());
        }
        return new ConsumerGroupOffsets(group, new HashSet<>(offsetsGroupedByThread.values()));
    }

    public void update(final TopicPartition tp,
                       final ConsumerThreadKey consumerThreadKey,
                       final Function<ConsumerLogOffsets, ConsumerLogOffsets> updater) {
        offsets.compute(tp, (k, tuple) -> {
            ConsumerLogOffsets newOffsets = updater.apply(tuple == null ? new ConsumerLogOffsets(tp) : tuple.right());
            return Tuple.of(consumerThreadKey, newOffsets);
        });
    }

    @VisibleForTesting
    Map<TopicPartition, Tuple<ConsumerThreadKey, ConsumerLogOffsets>> offsets() {
        return offsets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsumerGroupOffsetsState)) return false;
        ConsumerGroupOffsetsState that = (ConsumerGroupOffsetsState) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(offsets, that.offsets);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(group, offsets);
    }
}

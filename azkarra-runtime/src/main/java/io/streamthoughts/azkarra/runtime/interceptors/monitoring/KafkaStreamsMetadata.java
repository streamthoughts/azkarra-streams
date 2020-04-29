/*
 * Copyright 2019 StreamThoughts.
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
package io.streamthoughts.azkarra.runtime.interceptors.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.model.TimestampedValue;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerGroupOffsets;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.processor.ThreadMetadata;

import java.util.Objects;
import java.util.Set;

/**
 * Represents the runtime state of a single {@link KafkaStreams} instance.
 */
public class KafkaStreamsMetadata {

    private final TimestampedValue<State> state;

    private final Set<ThreadMetadata> threads;

    private final ConsumerGroupOffsets offsets;

    /**
     * Creates a new {@link KafkaStreamsMetadata} instance.
     *
     * @param state     the state of the {@link KafkaStreams instance.
     * @param threads   the set of {@link ThreadMetadata} of the {@link KafkaStreams instance.
     * @param offsets   the {@link ConsumerGroupOffsets} of the {@link KafkaStreams instance.
     */
    public KafkaStreamsMetadata(final TimestampedValue<State> state,
                         final Set<ThreadMetadata> threads,
                         final ConsumerGroupOffsets offsets) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.threads = Objects.requireNonNull(threads, "threads cannot be null");
        this.offsets = Objects.requireNonNull(offsets, "offsets cannot be null");
    }

    @JsonProperty("state")
    public State state() {
        return state.value();
    }

    @JsonProperty("state_changed_time")
    public long stateChangedTime() {
        return state.timestamp();
    }

    @JsonProperty("threads")
    public Set<ThreadMetadata> threads() {
        return threads;
    }

    @JsonProperty("offsets")
    public ConsumerGroupOffsets offsets() {
        return offsets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KafkaStreamsMetadata)) return false;
        KafkaStreamsMetadata that = (KafkaStreamsMetadata) o;
        return  Objects.equals(state, that.state) &&
                Objects.equals(threads, that.threads) &&
                Objects.equals(offsets, that.offsets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, threads, offsets);
    }

    @Override
    public String toString() {
        return "KafkaStreamsMetadata{" +
                "state=" + state +
                ", streamThreads=" + threads +
                ", offsets=" + offsets +
                '}';
    }
}

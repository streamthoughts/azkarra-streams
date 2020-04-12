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
package io.streamthoughts.azkarra.api.streams.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class ConsumerGroupOffsets {

    private final String group;
    private final Set<ConsumerClientOffsets> consumers;

    /**
     * Creates a new {@link ConsumerGroupOffsets} instance.
     *
     * @param group     the consumer group.
     * @param consumers the consumer offsets.
     */
    public ConsumerGroupOffsets(final String group,
                                final Set<ConsumerClientOffsets> consumers) {
        this.group = Objects.requireNonNull(group, "group cannot be null");
        this.consumers = Collections.unmodifiableSet(Objects.requireNonNull(consumers, "consumers cannot be null"));
    }

    @JsonProperty("group")
    public String group() {
        return group;
    }

    @JsonProperty("consumers")
    public Set<ConsumerClientOffsets> consumers() {
        return consumers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsumerGroupOffsets)) return false;
        ConsumerGroupOffsets that = (ConsumerGroupOffsets) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(consumers, that.consumers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(group, consumers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ConsumerGroupOffsets{ group=" + group +  ", consumers=" + consumers + '}';
    }
}

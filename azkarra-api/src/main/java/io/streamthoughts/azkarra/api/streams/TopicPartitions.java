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
package io.streamthoughts.azkarra.api.streams;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Set;

/**
 * This class represents topic-partitions assignment for a streams application.
 */
public class TopicPartitions implements Comparable<TopicPartitions> {

    private final String name;
    private final Set<Integer> partitions;

    /**
     * Creates a new {@link TopicPartitions} instance.
     *
     * @param name          the topic name.
     * @param partitions    the set of partitions.
     */
    public TopicPartitions(final String name,
                           final Set<Integer> partitions) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(partitions, "partitions cannot be null");
        this.name = name;
        this.partitions = partitions;
    }

    @JsonProperty
    public String name() {
        return name;
    }

    @JsonProperty
    public Set<Integer> partitions() {
        return partitions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopicPartitions)) return false;
        TopicPartitions that = (TopicPartitions) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(partitions, that.partitions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, partitions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final TopicPartitions that) {
        return this.name.compareTo(that.name);
    }
}

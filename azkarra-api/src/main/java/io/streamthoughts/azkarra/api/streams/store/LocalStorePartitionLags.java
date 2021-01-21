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
package io.streamthoughts.azkarra.api.streams.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.model.HasName;

import java.util.List;
import java.util.Objects;

public class LocalStorePartitionLags implements HasName {

    private final String name;

    private final List<PartitionLogOffsetsAndLag> positions;

    /**
     * Creates a new {@link LocalStorePartitionLags} instance.
     *
     * @param name          the local store name.
     * @param positions     the local store positions.
     */
    public LocalStorePartitionLags(final String name,
                                   final List<PartitionLogOffsetsAndLag> positions) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.positions = Objects.requireNonNull(positions, "positions cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty
    public String name() {
        return name;
    }

    @JsonProperty
    public List<PartitionLogOffsetsAndLag> positions() {
        return positions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalStorePartitionLags)) return false;
        LocalStorePartitionLags that = (LocalStorePartitionLags) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(positions, that.positions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, positions);
    }

    @Override
    public String toString() {
        return "LocalStorePartitionPositions{" +
                "name='" + name + '\'' +
                ", positions=" + positions +
                '}';
    }
}

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
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.streamthoughts.azkarra.api.model.HasName;
import io.streamthoughts.azkarra.commons.streams.StatePartitionRestoreInfo;

import java.util.List;
import java.util.Objects;

/**
 * The {@code LocalStatePartitionsInfo} describes the state of a state store.
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class LocalStatePartitionsInfo implements HasName {

    private final String name;

    private final List<StatePartitionRestoreInfo> partitionRestoreInfos;

    private final List<StatePartitionLagInfo> partitionLagInfos;

    /**
     * Creates a new {@link LocalStatePartitionsInfo} instance.
     *
     * @param name                  the name of the store.
     * @param partitionLagInfos     the list of {@link StatePartitionLagInfo}.
     * @param partitionRestoreInfos the list of {@link StatePartitionRestoreInfo}.
     */
    public LocalStatePartitionsInfo(final String name,
                                    final List<StatePartitionLagInfo> partitionLagInfos,
                                    final List<StatePartitionRestoreInfo> partitionRestoreInfos) {
        this.name = Objects.requireNonNull(name, "name should not be null");
        Objects.requireNonNull(partitionRestoreInfos, "partitionRestoreInfos should not be null");
        Objects.requireNonNull(partitionLagInfos, "partitionLagInfos should not be null");
        this.partitionLagInfos = partitionLagInfos;
        this.partitionRestoreInfos = partitionRestoreInfos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty
    public String name() {
        return name;
    }

    public List<StatePartitionLagInfo> getPartitionLagInfos() {
        return partitionLagInfos;
    }

    public List<StatePartitionRestoreInfo> getPartitionRestoreInfos() {
        return partitionRestoreInfos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalStatePartitionsInfo that = (LocalStatePartitionsInfo) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(partitionRestoreInfos, that.partitionRestoreInfos) &&
               Objects.equals(partitionLagInfos, that.partitionLagInfos);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, partitionRestoreInfos, partitionLagInfos);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "LocalStorePartitionsInfo{" +
                "name='" + name + '\'' +
                ", partitionRestoreInfos=" + partitionRestoreInfos +
                ", partitionLagInfos=" + partitionLagInfos +
                '}';
    }
}



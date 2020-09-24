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

import java.util.Objects;

public class PartitionLogOffsetsAndLag {

    /**
     * The log partition.
     */
    private final int partition;

    /**
     * The current offset.
     */
    private final long currentOffset;

    /**
     * The log end offset.
     */
    private final long logEndOffset;

    /**
     * The offset lag.
     */
    private final long lag;

    public PartitionLogOffsetsAndLag(final int partition,
                                     final long currentOffset,
                                     final long logEndOffset,
                                     final long offsetLag) {
        this.partition = partition;
        this.currentOffset = currentOffset;
        this.logEndOffset = logEndOffset;
        this.lag = offsetLag;
    }

    @JsonProperty("partition")
    public int partition() {
        return partition;
    }

    @JsonProperty("current_offset")
    public long currentOffset() {
        return currentOffset;
    }

    @JsonProperty("log_end_offset")
    public long logEndOffset() {
        return logEndOffset;
    }

    @JsonProperty("offset_lag")
    public long offsetLag() {
        return lag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartitionLogOffsetsAndLag)) return false;
        PartitionLogOffsetsAndLag that = (PartitionLogOffsetsAndLag) o;
        return partition == that.partition &&
                currentOffset == that.currentOffset &&
                logEndOffset == that.logEndOffset &&
                lag == that.lag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(partition, currentOffset, logEndOffset, lag);
    }

    @Override
    public String toString() {
        return "StorePartitionLogOffsets{" +
                "partition=" + partition +
                ", currentOffset=" + currentOffset +
                ", logEndOffset=" + logEndOffset +
                ", lag=" + lag +
                '}';
    }
}

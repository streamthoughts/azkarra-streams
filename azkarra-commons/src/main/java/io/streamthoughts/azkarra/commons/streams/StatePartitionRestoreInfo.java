/*
 * Copyright 2021 StreamThoughts.
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
package io.streamthoughts.azkarra.commons.streams;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;

/**
 * Describes the state of a restoration process for {@link TopicPartition}.
 *
 * @see LoggingStateRestoreListener
 * @see org.apache.kafka.streams.processor.StateRestoreListener
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class StatePartitionRestoreInfo {

    private final TopicPartition topicPartition;
    private final long startingOffset;
    private final long endingOffset;
    private long totalRestored;
    private Duration duration;

    /**
     * Creates a new {@link StatePartitionRestoreInfo} instance.
     *
     * @param topicPartition    the {@link TopicPartition} being restore.
     * @param startingOffset    the starting offset of the entire restoration process for this TopicPartition.
     * @param endingOffset      the exclusive ending offset of the entire restoration process for this TopicPartition.
     */
    public StatePartitionRestoreInfo(final TopicPartition topicPartition,
                                     final long startingOffset,
                                     final long endingOffset) {
        this.topicPartition = topicPartition;
        this.startingOffset = startingOffset;
        this.endingOffset = endingOffset;
        this.totalRestored = 0;
        this.duration = Duration.ZERO;
    }

    /**
     * Gets the Topic of the partition being restored.
     */
    public String getTopic() {
        return topicPartition.topic();
    }

    /**
     * Gets the Partition being restored.
     */
    public int getPartition() {
        return topicPartition.partition();
    }

    /**
     * Gets the starting offset of the entire restoration process for this TopicPartition.
     */
    public long getStartingOffset() {
        return startingOffset;
    }

    /**
     * Gets the exclusive ending offset of the entire restoration process for this TopicPartition
     */
    public long getEndingOffset() {
        return endingOffset;
    }

    /**
     * Gets the total number of records restored.
     */
    public long getTotalRestored() {
        return totalRestored;
    }

    /**
     * Increments the total number of records restored.
     *
     * @param numRestored  number of records restored.
     * @return the total restored.
     */
    public long incrementTotalRestored(final long numRestored) {
        this.totalRestored += numRestored;
        return totalRestored;
    }

    /**
     * Gets the duration of the restoration process.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Duration getDuration() {
        return duration;
    }

    /**
     * Sets the duration of the restoration process.
     */
    public void setDuration(final Duration duration) {
        this.duration = duration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "TopicPartitionRestoreInfo{" +
                "topicPartition=" + topicPartition +
                ", startingOffset=" + startingOffset +
                ", endingOffset=" + endingOffset +
                ", totalRestored=" + totalRestored +
                ", duration=" + duration +
                '}';
    }
}

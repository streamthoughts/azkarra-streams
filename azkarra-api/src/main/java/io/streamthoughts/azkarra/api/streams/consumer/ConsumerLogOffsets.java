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
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.apache.kafka.common.TopicPartition;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ConsumerLogOffsets implements Comparable<ConsumerLogOffsets> {

    private static final OffsetAndTimestamp EMPTY_OFFSET_AND_TIMESTAMP = new OffsetAndTimestamp(-1L, -1L);

    /**
     * The topic/partition.
     */
    private final TopicPartition topicPartition;

    /**
     * The last consumed offset/timestamp for this partition.
     */
    private OffsetAndTimestamp consumedOffset;

    /**
     * The last committed offset/timestamp for this partition.
     */
    private OffsetAndTimestamp committedOffset;

    /**
     * The log end offset.
     */
    private Long logEndOffset;

    /**
     * The log start offset.
     */
    private Long logStartOffset;

    ConsumerLogOffsets(final String topic, final int partition) {
        this(
            new TopicPartition(topic, partition),
            EMPTY_OFFSET_AND_TIMESTAMP,
            EMPTY_OFFSET_AND_TIMESTAMP,
            -1L,
            0L
        );
    }


    ConsumerLogOffsets(final TopicPartition topicPartition) {
        this(topicPartition, EMPTY_OFFSET_AND_TIMESTAMP, EMPTY_OFFSET_AND_TIMESTAMP, -1L, 0L);
    }

    private ConsumerLogOffsets(final TopicPartition topicPartition,
                               final OffsetAndTimestamp consumedOffset,
                               final OffsetAndTimestamp committedOffset,
                               final Long logEndOffset,
                               final Long logStartOffset) {
        this.topicPartition = topicPartition;
        this.consumedOffset = consumedOffset;
        this.committedOffset = committedOffset;
        this.logEndOffset = logEndOffset;
        this.logStartOffset = logStartOffset;
    }

    @JsonUnwrapped
    @JsonProperty
    public TopicPartition topicPartition() {
        return topicPartition;
    }

    @JsonProperty
    public Long logEndOffset() {
        return logEndOffset;
    }

    @JsonProperty
    public Long logStartOffset() {
        return logStartOffset;
    }

    @JsonProperty
    public Long lag() {
        if (logEndOffset != -1 && !consumedOffset.equals(EMPTY_OFFSET_AND_TIMESTAMP) )
            // The log end offset correspond to next offsets that will be used.
            return Math.max(logEndOffset - consumedOffset.offset() - 1, 0);
        else
            return -1L;
    }

    @JsonProperty
    @JsonUnwrapped(prefix = "consumed")
    public OffsetAndTimestamp consumedOffset() {
        return consumedOffset;
    }

    @JsonProperty
    @JsonUnwrapped(prefix = "committed")
    public OffsetAndTimestamp committedOffset() {
        return committedOffset;
    }

    public ConsumerLogOffsets consumedOffset(final OffsetAndTimestamp consumedOffset) {
        return new ConsumerLogOffsets(topicPartition, consumedOffset, committedOffset, logEndOffset, logStartOffset);
    }

    public ConsumerLogOffsets committedOffset(final OffsetAndTimestamp committedOffset) {
        return new ConsumerLogOffsets(topicPartition, consumedOffset, committedOffset, logEndOffset, logStartOffset);
    }

    public ConsumerLogOffsets logEndOffset(final Long logEndOffset) {
        return new ConsumerLogOffsets(topicPartition, consumedOffset, committedOffset, logEndOffset, logStartOffset);
    }

    public ConsumerLogOffsets logStartOffset(final Long logStartOffset) {
        return new ConsumerLogOffsets(topicPartition, consumedOffset, committedOffset, logEndOffset, logStartOffset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsumerLogOffsets)) return false;
        ConsumerLogOffsets that = (ConsumerLogOffsets) o;
        return Objects.equals(topicPartition, that.topicPartition) &&
                Objects.equals(consumedOffset, that.consumedOffset) &&
                Objects.equals(committedOffset, that.committedOffset) &&
                Objects.equals(logEndOffset, that.logEndOffset) &&
                Objects.equals(logStartOffset, that.logStartOffset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(topicPartition, consumedOffset, committedOffset, logEndOffset, logStartOffset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ConsumerLogOffsets{" +
                "topicPartition=" + topicPartition +
                ", consumedOffset=" + consumedOffset +
                ", committedOffset=" + committedOffset +
                ", logEndOffset=" + logEndOffset +
                ", logStartOffset=" + logStartOffset +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final ConsumerLogOffsets that) {
        return Integer.compare(this.topicPartition.partition(), that.topicPartition.partition());
    }
}

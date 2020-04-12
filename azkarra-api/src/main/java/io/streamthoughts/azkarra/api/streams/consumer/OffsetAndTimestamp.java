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

import io.streamthoughts.azkarra.api.time.Time;

import java.util.Objects;

public class OffsetAndTimestamp {

    private final long offset;
    private final long timestamp;

    /**
     * Creates a new {@link OffsetAndTimestamp} instance.
     *
     * @param offset        the offset.
     */
    OffsetAndTimestamp(final long offset) {
        this(offset, Time.SYSTEM.milliseconds());
    }


    /**
     * Creates a new {@link OffsetAndTimestamp} instance.
     *
     * @param offset        the offset.
     * @param timestamp     the timestamp.
     */
    OffsetAndTimestamp(final long offset, final long timestamp) {
        this.offset = offset;
        this.timestamp = timestamp;
    }

    public long offset() {
        return offset;
    }

    public long timestamp() {
        return timestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OffsetAndTimestamp)) return false;
        OffsetAndTimestamp that = (OffsetAndTimestamp) o;
        return offset == that.offset &&
                timestamp == that.timestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(offset, timestamp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[offset=" + offset + ", timestamp=" + timestamp + ']';
    }
}

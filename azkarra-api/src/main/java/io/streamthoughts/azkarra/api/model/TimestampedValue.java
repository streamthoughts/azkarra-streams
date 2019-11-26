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
package io.streamthoughts.azkarra.api.model;

import io.streamthoughts.azkarra.api.time.Time;

import java.util.Objects;

public class TimestampedValue<V> implements Comparable<TimestampedValue> {

    private final long timestamp;

    private final V value;

    /**
     * Creates a new {@link TimestampedValue} instance.
     *
     * @param value     the value.
     */
    public TimestampedValue(final V value) {
        this(Time.SYSTEM.milliseconds(), value);
    }


    /**
     * Creates a new {@link TimestampedValue} instance.
     *
     * @param timestamp the timestamp.
     * @param value     the value.
     */
    public TimestampedValue(final long timestamp, final V value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    /**
     * Returns the timestamp in milliseconds for this value.
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns this value {@link V}.
     */
    public V value() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimestampedValue)) return false;
        TimestampedValue<?> that = (TimestampedValue<?>) o;
        return timestamp == that.timestamp &&
                Objects.equals(value, that.value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "TimestampedValue{" +
                "timestamp=" + timestamp +
                ", value=" + value +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final TimestampedValue that) {
        return Long.compare(this.timestamp, that.timestamp);
    }
}

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

import io.streamthoughts.azkarra.api.time.Time;

import java.util.Objects;

public class StateChangeEvent {

    private final long timestamp;
    private final State newState;
    private final State oldState;

    /**
     * Creates a new {@link StateChangeEvent} instance.
     *
     * @param newState  the new {@link State}.
     * @param oldState  the old {@link State}.
     */
    public StateChangeEvent(final State newState,
                            final State oldState) {
        this(Time.SYSTEM.milliseconds(), newState, oldState);
    }

    /**
     * Creates a new {@link StateChangeEvent} instance.
     *
     * @param timestamp the new state timestamp.
     * @param newState  the new {@link State}.
     * @param oldState  the old {@link State}.
     */
    public StateChangeEvent(final long timestamp,
                            final State newState,
                            final State oldState) {
        this.timestamp = timestamp;
        this.newState = newState;
        this.oldState = oldState;
    }

    public long timestamp() {
        return timestamp;
    }

    public State newState() {
        return newState;
    }

    public State oldState() {
        return oldState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StateChangeEvent)) return false;
        StateChangeEvent that = (StateChangeEvent) o;
        return timestamp == that.timestamp &&
                newState == that.newState &&
                oldState == that.oldState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, newState, oldState);
    }

    @Override
    public String toString() {
        return "StateChangeEvent{" +
                "timestamp=" + timestamp +
                ", newState=" + newState +
                ", oldState=" + oldState +
                '}';
    }
}

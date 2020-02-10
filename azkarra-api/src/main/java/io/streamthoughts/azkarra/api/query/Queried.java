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
package io.streamthoughts.azkarra.api.query;

import java.time.Duration;
import java.util.Objects;

public class Queried {

    public static Queried with(final Duration timeout) {
        return new Queried(0, Duration.ZERO, timeout, true, -1L);
    }

    public static Queried locally() {
        return new Queried(0, Duration.ZERO, Duration.ZERO,false, -1L);
    }

    public static Queried immediately() {
        return new Queried(0, Duration.ZERO, Duration.ZERO,true, -1L);
    }

    public static Queried retries(final int retries, final Duration retryBackoff) {
        Duration queryTimeout = retryBackoff.multipliedBy(retries + 1);
        return new Queried(retries, retryBackoff, queryTimeout, true, -1L);
    }

    /**
     * The maximum number of attempts to run after failed access to a given local state store.
     */
    private final int retries;

    /**
     *  The time to wait before attempting to retry a failed access to a given local state store.
     */
    private final Duration retryBackoff;

    /**
     * This limit the total time of state store execute.
     */
    private final Duration queryTimeout;

    /**
     * Is remote access is allowed for this execute.
     */
    private final boolean remoteAccessAllowed;

    /**
     * Only return the first n records.
     */
    private final Long limit;

    /**
     * Creates a new {@link Queried} instance.
     *
     * @param retries               {@link #retries}.
     * @param retryBackoff          {@link #retryBackoff}.
     * @param queryTimeout          {@link #queryTimeout}.
     * @param remoteAccessAllowed   {@link #retryBackoff}.
     * @param limit                 {@link #limit}.
     */
    public Queried(final int retries,
                   final Duration retryBackoff,
                   final Duration queryTimeout,
                   final boolean remoteAccessAllowed,
                   final Long limit) {
        this.retries = retries;
        this.retryBackoff = retryBackoff;
        this.queryTimeout = queryTimeout;
        this.remoteAccessAllowed = remoteAccessAllowed;
        this.limit = limit;
    }

    public Queried withRemoteAccessAllowed(final boolean remoteAccessAllowed) {
        return new Queried(retries, retryBackoff, queryTimeout, remoteAccessAllowed, limit);
    }

    public Queried withQueryTimeout(final Duration timeout) {
        return new Queried(retries, retryBackoff, timeout, remoteAccessAllowed, limit);
    }

    public Queried withRetries(final int retries) {
        return new Queried(retries, retryBackoff, queryTimeout, remoteAccessAllowed, limit);
    }

    public Queried withRetryBackoffMs(final Duration retryBackoff) {
        return new Queried(retries, retryBackoff, queryTimeout, remoteAccessAllowed, limit);
    }

    public Queried withLimit(final Long limit) {
        return new Queried(retries, retryBackoff, queryTimeout, remoteAccessAllowed, limit);
    }

    /**
     * Gets the maximum number of attempts.
     *
     * @return the number retries.
     */
    public int retries() {
        return retries;
    }

    /**
     * Gets the time interval before attempting a new operation.
     *
     * @return the retry backoff.
     */
    public Duration retryBackoff() {
        return retryBackoff;
    }

    /**
     * Gets whether remote access is allowed.
     *
     * @return {@code true} if remote access if allowed, {@code false} otherwise.
     */
    public boolean remoteAccessAllowed() {
        return remoteAccessAllowed;
    }

    /**
     * Gets the maximum duration for executing the execute.
     *
     * @return the timeout.
     */
    public Duration queryTimeout() {
        return queryTimeout;
    }

    /**
     * Gets the maximum number of records to return. -1 is infinite.
     *
     * @return  the limit.
     */
    public Long limit() {
        return limit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Queried)) return false;
        Queried queried = (Queried) o;
        return retries == queried.retries &&
                remoteAccessAllowed == queried.remoteAccessAllowed &&
                Objects.equals(retryBackoff, queried.retryBackoff) &&
                Objects.equals(queryTimeout, queried.queryTimeout) &&
                Objects.equals(limit, queried.limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(retries, retryBackoff, queryTimeout, remoteAccessAllowed, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Queried{" +
                "retries=" + retries +
                ", retryBackoff=" + retryBackoff +
                ", queryTimeout=" + queryTimeout +
                ", remoteAccessAllowed=" + remoteAccessAllowed +
                ", limit=" + limit +
                '}';
    }
}

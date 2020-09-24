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
package io.streamthoughts.azkarra.api.monad;

import java.time.Duration;

public class Retry {

    public static Retry withMaxAttempts(int maxAttempts) {
        return new Retry(null, 0L, Long.MAX_VALUE, maxAttempts);
    }

    private long fixedWaitMs;
    private long stopAfterMs;
    private int maxAttempts;
    private Class<? extends Exception> exceptionType;

    /**
     * Creates a new {@link Retry} instance.
     */
    private Retry(final Class<? extends Exception> exceptionType,
                  final long fixedWaitMs,
                  final long stopAfterMs,
                  final int maxAttempts) {
        this.exceptionType = exceptionType;
        this.fixedWaitMs = fixedWaitMs;
        this.stopAfterMs = stopAfterMs;
        this.maxAttempts = maxAttempts;
    }

    public Retry ifExceptionOfType(final Class<? extends Exception> exception) {
        return new Retry(exception, fixedWaitMs, stopAfterMs, maxAttempts);
    }

    public Retry withFixedWaitDuration(final Duration fixedWaitMs) {
        return new Retry(exceptionType, fixedWaitMs.toMillis(), stopAfterMs, maxAttempts);
    }

    public Retry stopAfterMaxAttempts(final int maxAttempts) {
        return new Retry(exceptionType, fixedWaitMs, stopAfterMs, maxAttempts);
    }

    public Retry stopAfterDuration(final Duration stopAfterMs) {
        return new Retry(exceptionType, fixedWaitMs, stopAfterMs.toMillis(), maxAttempts);
    }

    long fixedWaitMs() {
        return fixedWaitMs;
    }

    long stopAfterMs() {
        return stopAfterMs;
    }

    int maxAttempts() {
        return maxAttempts;
    }

    public Class<? extends Exception> exceptionType() {
        return exceptionType;
    }

    boolean isRetriable(final Throwable e) {
        return exceptionType == null || exceptionType.isAssignableFrom(e.getClass());
    }

    @Override
    public String toString() {
        return "Retry{" +
                "fixedWaitMs=" + fixedWaitMs +
                ", stopAfterMs=" + stopAfterMs +
                ", maxAttempts=" + maxAttempts +
                ", exceptionType=" + exceptionType +
                '}';
    }
}

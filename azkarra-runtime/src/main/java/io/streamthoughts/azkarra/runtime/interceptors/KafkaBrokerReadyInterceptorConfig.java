/*
 * Copyright 2020 StreamThoughts.
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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Objects;

public class KafkaBrokerReadyInterceptorConfig {

    /** {@code kafka.ready.interceptor.enable} */
    public static final String KAFKA_READY_INTERCEPTOR_ENABLE_CONFIG = "kafka.ready.interceptor.enable";

    /** {@code kafka.ready.interceptor.timeout.ms} */
    public static final String KAFKA_READY_INTERCEPTOR_TIMEOUT_MS_CONFIG = "kafka.ready.interceptor.timeout.ms";
    public static final long KAFKA_READY_INTERCEPTOR_TIMEOUT_MS_DEFAULT = 60_000L;

    /** {@code kafka.ready.interceptor.connect.backoff.ms} */
    public static final String KAFKA_READY_INTERCEPTOR_RETRY_BACKOFF_MS_CONFIG = "kafka.ready.interceptor.retry.backoff.ms";
    public static final long KAFKA_READY_INTERCEPTOR_RETRY_BACKOFF_MS_DEFAULT = 1_000;

    /** {@code kafka.ready.interceptor.min.available.brokers} */
    public static final String KAFKA_READY_INTERCEPTOR_MIN_AVAILABLE_BROKERS_CONFIG = "kafka.ready.interceptor.min.available.brokers";
    public static final int KAFKA_READY_INTERCEPTOR_MIN_AVAILABLE_BROKERS_DEFAULT = 1;

    private final Conf originals;

    /**
     * Creates a new {@link KafkaBrokerReadyInterceptorConfig} instance.
     *
     * @param originals the {@link Conf}.
     */
    public KafkaBrokerReadyInterceptorConfig(final Conf originals) {
        this.originals = Objects.requireNonNull(originals);
    }

    public long getTimeoutMs() {
        return originals
            .getOptionalLong(KAFKA_READY_INTERCEPTOR_TIMEOUT_MS_CONFIG)
            .orElse(KAFKA_READY_INTERCEPTOR_TIMEOUT_MS_DEFAULT);
    }

    public long getRetryBackoffMs() {
        return originals
            .getOptionalLong(KAFKA_READY_INTERCEPTOR_RETRY_BACKOFF_MS_CONFIG)
            .orElse(KAFKA_READY_INTERCEPTOR_RETRY_BACKOFF_MS_DEFAULT);
    }

    public int getMinAvailableBrokers() {
        return originals
            .getOptionalInt(KAFKA_READY_INTERCEPTOR_MIN_AVAILABLE_BROKERS_CONFIG)
            .orElse(KAFKA_READY_INTERCEPTOR_MIN_AVAILABLE_BROKERS_DEFAULT);
    }
}

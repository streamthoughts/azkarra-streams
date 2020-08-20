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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.config.Conf;

import java.time.Duration;
import java.util.Objects;

/**
 * The configuration class for {@link WaitForSourceTopicsInterceptor}.
 */
public class WaitForSourceTopicsInterceptorConfig {

    /**
     * {@code auto.create.topics.configs}
     */
    public static String WAIT_FOR_TOPICS_ENABLE_CONFIG    = "enable.wait.for.topics";

    /**
     * {@code wait.for.topics.timeout.ms}
     */
    public static String WAIT_FOR_TOPICS_TIMEOUT_MS_CONFIG = "wait.for.topics.timeout.ms";
    public static long  WAIT_FOR_TOPICS_TIMEOUT_MS_DEFAULT = Long.MAX_VALUE;

    private final Conf originals;
    /**
     * Creates a new {@link AutoCreateTopicsInterceptorConfig} instance.
     *
     * @param originals the {@link Conf} instance.
     */
    public WaitForSourceTopicsInterceptorConfig(final Conf originals) {
        this.originals = Objects.requireNonNull(originals, "originals config cannot be null");
    }

    public Duration getTimeout() {
        final Long millis = originals
                .getOptionalLong(WAIT_FOR_TOPICS_TIMEOUT_MS_CONFIG)
                .orElse(WAIT_FOR_TOPICS_TIMEOUT_MS_DEFAULT);
        return Duration.ofMillis(millis);
    }
}
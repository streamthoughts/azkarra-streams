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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The configuration class for {@link WaitForSourceTopicsInterceptor}.
 */
public class WaitForSourceTopicsInterceptorConfig {

    /**
     * {@code wait.for.topics.enable}
     */
    public static String ENABLE_WAIT_FOR_TOPICS__CONFIG  = "enable.wait.for.topics";
    public static String WAIT_FOR_TOPICS_ENABLE_CONFIG   = "wait.for.topics.enable";

    /**
     * {@code wait.for.topics.timeout.ms}
     */
    public static String WAIT_FOR_TOPICS_TIMEOUT_MS_CONFIG = "wait.for.topics.timeout.ms";
    public static long  WAIT_FOR_TOPICS_TIMEOUT_MS_DEFAULT = Long.MAX_VALUE;

    /**
     * {@code wait.for.topics.exclude.patterns}
     */
    public static String WAIT_FOR_TOPICS_EXCLUDE_PATTERNS = "wait.for.topics.exclude.patterns";

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

    public List<Pattern> getExcludePatterns() {
        if (originals.hasPath(WAIT_FOR_TOPICS_EXCLUDE_PATTERNS)) {
            return originals
                .getStringList(WAIT_FOR_TOPICS_EXCLUDE_PATTERNS)
                .stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
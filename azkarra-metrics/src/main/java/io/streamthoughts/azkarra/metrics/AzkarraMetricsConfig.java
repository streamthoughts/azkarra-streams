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
package io.streamthoughts.azkarra.metrics;

import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Objects;

/**
 * Properties to configure metrics.
 */
public class AzkarraMetricsConfig {

    public static final String METRICS_ENABLE_CONFIG = "metrics.enable";
    public static final String METRICS_BINDERS_JVM_ENABLE_CONFIG = "metrics.binders.jvm.enable";
    public static final String METRICS_BINDERS_KAFKA_STREAMS_ENABLE_CONFIG = "metrics.binders.kafkastreams.enable";
    public static final String METRICS_ENDPOINT_PROMETHEUS_ENABLE_CONFIG = "metrics.endpoints.prometheus.enable";

    private final Conf config;

    /**
     * Creates a new {@link AzkarraMetricsConfig} instance.
     * @param config    the {@link Conf}.
     */
    public AzkarraMetricsConfig(final Conf config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }

    public boolean isEnable(final String name) {
        return config.getOptionalBoolean(name).orElse(false);
    }
}

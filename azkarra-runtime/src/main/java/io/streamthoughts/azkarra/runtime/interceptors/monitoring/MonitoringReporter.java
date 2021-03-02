/*
 * Copyright 2021 StreamThoughts.
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
package io.streamthoughts.azkarra.runtime.interceptors.monitoring;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;

/**
 * A {@code MonitoringStreamsReporter} can be used to periodically report the state of
 * a local {@link org.apache.kafka.streams.KafkaStreams} instance.
 *
 * Implementations can implement the interface {@link io.streamthoughts.azkarra.api.streams.KafkaStreamsContainerAware}.
 *
 * @see KafkaStreamsMetadata
 * @see MonitoringStreamsTask
 * @see io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptor
 *
 * Called periodically by the monitoring tasks.
 */
public interface MonitoringReporter extends AutoCloseable, Configurable {

    /**
     * Called periodically by the monitoring tasks.
     * This method can be called after {@link #close()} has been invoked if the stream instance is restarted.
     *
     * @param metadata  the {@link KafkaStreamsMetadata} change to report.
     */
    void report(final KafkaStreamsMetadata metadata);

    /**
     * Configure this {@code StreamsMetadataReporter}.
     *
     * @param configuration  the {@link Conf} instance used to configure this instance.
     */
    @Override
    default void configure(final Conf configuration) {
    }

    /**
     * Closes this {@link MonitoringReporter}.
     * Called when a {@link io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer} is stopped.
     */
    @Override
    default void close() {
    }

}

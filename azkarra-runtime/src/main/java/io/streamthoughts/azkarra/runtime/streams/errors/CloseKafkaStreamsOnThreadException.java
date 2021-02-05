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
package io.streamthoughts.azkarra.runtime.streams.errors;

import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironmentAware;
import io.streamthoughts.azkarra.api.ApplicationId;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.errors.StreamThreadExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Closes immediately the {@link org.apache.kafka.streams.KafkaStreams} instance when a StreamThread
 * abruptly terminates due to an uncaught exception.
 */
public class CloseKafkaStreamsOnThreadException implements
        StreamThreadExceptionHandler,
        StreamsExecutionEnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(CloseKafkaStreamsOnThreadException.class);

    private StreamsExecutionEnvironment<?> environment;

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final KafkaStreamsContainer container,
                       final Thread streamThread,
                       final Throwable e) {
        final ApplicationId id = new ApplicationId(container.applicationId());
        LOG.error(
            "The instance '{}' may be in error state. " +
            "A stream thread died due to uncaught exception. Stopping application immediately.",
            container.applicationId(),
            e
        );
        this.environment.stop(id, false, Duration.ZERO);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExecutionEnvironment(final StreamsExecutionEnvironment<?> environment) {
        this.environment = environment;
    }
}

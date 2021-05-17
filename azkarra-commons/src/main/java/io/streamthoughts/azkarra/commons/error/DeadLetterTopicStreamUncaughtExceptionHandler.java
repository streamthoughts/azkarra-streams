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
package io.streamthoughts.azkarra.commons.error;

import io.streamthoughts.azkarra.commons.error.internal.FailedRecordContextBuilder;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.streamthoughts.azkarra.commons.error.ExceptionType.STREAM;

/**
 * The {@code DeadLetterTopicStreamUncaughtExceptionHandler} can be used to send corrupted records
 * to a dead-letter-topic.
 *
 * @see DeserializationExceptionHandler
 */
public class DeadLetterTopicStreamUncaughtExceptionHandler
        extends AbstractDeadLetterTopicExceptionHandler implements StreamsUncaughtExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DeadLetterTopicStreamUncaughtExceptionHandler.class);

    /**
     * Creates a new {@link DeadLetterTopicStreamUncaughtExceptionHandler} instance.
     */
    public DeadLetterTopicStreamUncaughtExceptionHandler() {
        super(STREAM);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Map<String, ?> configs) {
        super.configure(configs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamThreadExceptionResponse handle(final Throwable exception) {

        final DeadLetterTopicNameExtractor extractor = config().topicNameExtractor();
        final String extractedOutputTopic =  extractor.extract(
                null,
                null,
                FailedRecordContextBuilder.with(exception, STREAM).build()
        );

        if (GlobalDeadLetterTopicCollector.isCreated()) {
            final Failed failed = Failed.withStreamError(applicationId(), exception);
            GlobalDeadLetterTopicCollector.get().send(extractedOutputTopic, failed);
        } else {
            LOG.warn("Failed to send corrupted record to Dead Letter Topic. "
                    + "GlobalDeadLetterTopicCollector is not initialized.");
        }

        final ExceptionHandlerResponse response = getHandlerResponseForExceptionOrElse(
                exception,
                ExceptionHandlerResponse.CONTINUE
        );

        switch (response) {
            case REPLACE_THREAD:
                return StreamThreadExceptionResponse.REPLACE_THREAD;
            case SHUTDOWN_CLIENT:
                return StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
            case SHUTDOWN_APPLICATION:
                return StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
            default:
                throw new IllegalArgumentException("Unsupported StreamThreadExceptionResponse: " + response);
        }
    }
}

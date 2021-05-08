/*
 * Copyright 2019-2021 StreamThoughts.
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

import io.streamthoughts.azkarra.commons.error.internal.AbstractDeadLetterTopicExceptionHandler;
import io.streamthoughts.azkarra.commons.error.internal.FailedRecordContextBuilder;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.errors.ProductionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.streamthoughts.azkarra.commons.error.ExceptionType.DESERIALIZATION;

/**
 * The {@code DeadLetterTopicProductionExceptionHandler} can be used to send a message to dead-letter-topic
 * when an exception happens while producing sink record.
 *
 * @see ProductionExceptionHandler
 */
public class DeadLetterTopicProductionExceptionHandler
        extends AbstractDeadLetterTopicExceptionHandler implements ProductionExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DeadLetterTopicProductionExceptionHandler.class);

    /**
     * Creates a new {@link DeadLetterTopicProductionExceptionHandler} instance.
     */
    public DeadLetterTopicProductionExceptionHandler() {
        super(ExceptionType.PRODUCTION);
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
    public ProductionExceptionHandlerResponse handle(
            final ProducerRecord<byte[], byte[]> record, final Exception exception) {

        final HandlerResponse response =
                getHandlerResponseForExceptionOrElse(exception, HandlerResponse.FAIL);

        final String extractedOutputTopic = extractOutputTopicName(record, exception);

        LOG.error(
                "Failed to produce output record to '{}-{}': {}.",
                record.topic(),
                record.partition(),
                response,
                exception);

        if (GlobalDeadLetterTopicManager.initialized()) {
            final ProducerRecord<byte[], byte[]> producerRecord =
                    new ProducerRecord<>(
                            extractedOutputTopic,
                            null,
                            record.timestamp(),
                            record.key(),
                            record.value(),
                            getEnrichedHeaders(record, exception));

            GlobalDeadLetterTopicManager.send(producerRecord);
        } else {
            LOG.warn("Failed to send corrupted record to Dead Letter Topic. "
                         + "GlobalDeadLetterTopicManager is not initialized.");
        }

        switch (response) {
            case CONTINUE:
                return ProductionExceptionHandlerResponse.CONTINUE;
            case FAIL:
                return ProductionExceptionHandlerResponse.FAIL;
            default:
                throw new IllegalArgumentException("Unsupported HandlerResponse: " + response);
        }
    }

    private Headers getEnrichedHeaders(final ProducerRecord<byte[], byte[]> record,
                                       final Exception exception) {
        return ExceptionHeaders.addExceptionHeaders(
                record.headers(),
                Failed.withProductionError(applicationId(), exception)
                        .withRecordTopic(record.topic())
                        .withRecordPartition(record.partition())
                        .withRecordType(Failed.RecordType.SINK)
                        .withCustomHeaders(customHeaders()));
    }

    private String extractOutputTopicName(final ProducerRecord<byte[], byte[]> record,
                                          final Exception exception) {
        final DeadLetterTopicNameExtractor extractor = config().topicNameExtractor();
        final FailedRecordContext recordContext = FailedRecordContextBuilder.with(exception, DESERIALIZATION)
                .withTopic(record.topic())
                .withPartition(record.partition())
                .withTimestamp(record.timestamp())
                .withHeaders(record.headers())
                .build();

        return extractor.extract(record.key(), record.value(), recordContext);
    }
}

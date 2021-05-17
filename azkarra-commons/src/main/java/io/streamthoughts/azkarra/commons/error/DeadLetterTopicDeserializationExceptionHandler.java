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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.streamthoughts.azkarra.commons.error.ExceptionType.DESERIALIZATION;

/**
 * The {@code DeadLetterTopicDeserializationExceptionHandler} can be used to send corrupted records
 * to a dead-letter-topic.
 *
 * @see DeserializationExceptionHandler
 */
public class DeadLetterTopicDeserializationExceptionHandler
        extends AbstractDeadLetterTopicExceptionHandler implements DeserializationExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DeadLetterTopicDeserializationExceptionHandler.class);

    /**
     * Creates a new {@link DeadLetterTopicDeserializationExceptionHandler} instance.
     */
    public DeadLetterTopicDeserializationExceptionHandler() {
        super(DESERIALIZATION);
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
    public DeserializationHandlerResponse handle(final ProcessorContext context,
                                                 final ConsumerRecord<byte[], byte[]> record,
                                                 final Exception exception) {

        final String extractedOutputTopic = extractOutputTopicName(record, exception);

        LOG.debug(
                "Sending rejected record from topic={}, partition={}, offset={} into topic {}",
                record.topic(),
                record.partition(),
                record.offset(),
                extractedOutputTopic);

        if (GlobalDeadLetterTopicCollector.isCreated()) {
            final Failed failed = Failed.withDeserializationError(applicationId(), exception)
                    .withRecordType(Failed.RecordType.SOURCE)
                    .withRecordTopic(record.topic())
                    .withRecordPartition(record.partition())
                    .withRecordOffset(record.offset())
                    .withRecordTimestamp(record.timestamp())
                    .withRecordHeaders(record.headers());
            GlobalDeadLetterTopicCollector.get().send(
                    extractedOutputTopic,
                    record.key(),
                    record.value(),
                    Serdes.ByteArray().serializer(),
                    Serdes.ByteArray().serializer(),
                    failed
            );
        } else {
            LOG.warn("Failed to send corrupted record to Dead Letter Topic. "
                    + "GlobalDeadLetterTopicCollector is not initialized.");
        }

        final ExceptionHandlerResponse response = getHandlerResponseForExceptionOrElse(
                exception,
                ExceptionHandlerResponse.CONTINUE
        );

        switch (response) {
            case CONTINUE:
                return DeserializationHandlerResponse.CONTINUE;
            case FAIL:
                return DeserializationHandlerResponse.FAIL;
            default:
                throw new IllegalArgumentException("Unsupported DeserializationHandlerResponse: " + response);
        }
    }

    private String extractOutputTopicName(final ConsumerRecord<byte[], byte[]> record,
                                          final Exception exception) {
        final DeadLetterTopicNameExtractor extractor = config().topicNameExtractor();
        final FailedRecordContext recordContext = FailedRecordContextBuilder.with(exception, DESERIALIZATION)
                .withTopic(record.topic())
                .withPartition(record.partition())
                .withTimestamp(record.timestamp())
                .withOffset(record.offset())
                .withHeaders(record.headers())
                .build();
        return extractor.extract(record.key(), record.value(), recordContext);
    }
}

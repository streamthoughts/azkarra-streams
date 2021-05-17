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
package io.streamthoughts.azkarra.commons.error.internal;

import io.streamthoughts.azkarra.commons.error.ExceptionType;
import io.streamthoughts.azkarra.commons.error.FailedRecordContext;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.processor.RecordContext;

/**
 * The {@link FailedRecordContextBuilder} can be used to build a new {@link FailedRecordContext}.
 *
 * @see io.streamthoughts.azkarra.commons.error.DeadLetterTopicNameExtractor
 */
public final class FailedRecordContextBuilder {

    private Throwable exception;
    private ExceptionType exceptionType;
    private Long offset;
    private Long timestamp;
    private Integer partition;
    private String topic;
    private Headers headers;

    /**
     * Creates a new {@link FailedRecordContextBuilder} for the given exception.
     *
     * @param exception     the exception
     * @param exceptionType the type of the exception.
     * @return              a new {@link FailedRecordContextBuilder}.
     */
    public static FailedRecordContextBuilder with(final Throwable exception,
                                                  final ExceptionType exceptionType) {
        return new FailedRecordContextBuilder(exception, exceptionType);

    }

    /**
     * Creates a new {@link FailedRecordContextBuilder} for the given exception and {@link RecordContext}.
     *
     * @param exception     the exception
     * @param exceptionType the type of the exception.
     * @return              a new {@link FailedRecordContextBuilder}.
     */
    public static FailedRecordContextBuilder with(final Throwable exception,
                                                  final ExceptionType exceptionType,
                                                  final RecordContext context) {

        return with(exception, exceptionType)
                .withTopic(context.topic())
                .withPartition(context.partition())
                .withOffset(context.offset())
                .withTimestamp(context.timestamp())
                .withHeaders(context.headers());
    }

    /**
     * Creates a new {@link FailedRecordContextBuilder} instance.
     * @param exception         the exception.
     * @param exceptionType     the type of the exception.
     */
    private FailedRecordContextBuilder(final Throwable exception,
                                       final ExceptionType exceptionType) {
        this.exception = exception;
        this.exceptionType = exceptionType;
    }

    public FailedRecordContextBuilder withException(final Exception exception) {
        this.exception = exception;
        return this;
    }

    public FailedRecordContextBuilder withExceptionType(final ExceptionType exceptionType) {
        this.exceptionType = exceptionType;
        return this;
    }

    public FailedRecordContextBuilder withTopic(final String topic) {
        this.topic = topic;
        return this;
    }

    public FailedRecordContextBuilder withTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public FailedRecordContextBuilder withOffset(final Long offset) {
        this.offset = offset;
        return this;
    }

    public FailedRecordContextBuilder withPartition(final Integer partition) {
        this.partition = partition;
        return this;
    }

    public FailedRecordContextBuilder withHeaders(final Headers headers) {
        this.headers = headers;
        return this;
    }

    /**
     * @return a new {@link FailedRecordContext}.
     */
    public FailedRecordContext build() {
        return new InternalFailedRecordContext(
                exception,
                exceptionType,
                offset,
                timestamp,
                partition,
                topic,
                headers
        );
    }
}

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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.kafka.common.header.Header;

public class Failed {

    public enum RecordType {
        SOURCE, SINK
    }

    /**
     * Helper method to create a new {@link Failed} for the given deserialization exception.
     *
     * @param applicationId the stream application id, i.e., {@code application.id}.
     * @param exception     the exception that was handled.
     * @return a new {@link Failed} instance.
     */
    public static Failed withDeserializationError(final String applicationId, final Exception exception) {
        return new Failed(applicationId, exception, ExceptionType.DESERIALIZATION);
    }

    /**
     * Helper method to create a new {@link Failed} for the given production exception.
     *
     * @param applicationId the stream application id, i.e., {@code application.id}.
     * @param exception     the exception that was handled.
     * @return a new {@link Failed} instance.
     */
    public static Failed withProductionError(final String applicationId, final Exception exception) {
        return new Failed(applicationId, exception, ExceptionType.PRODUCTION);
    }

    private final Exception exception;
    private final String applicationId;
    private final ExceptionType exceptionTypes;
    private String topic;
    private Integer partition;
    private Long offset;
    private RecordType recordType;

    private List<Header> customHeaders = Collections.emptyList();

    /**
     * Creates a new {@link Failed} instance.
     *
     * @param applicationId  the stream application id, i.e., {@code application.id}.
     * @param exception      the exception that was handled.
     * @param exceptionTypes the exception type.
     */
    public Failed(final String applicationId,
                  final Exception exception,
                  final ExceptionType exceptionTypes) {
        this.applicationId = Objects.requireNonNull(applicationId, "'applicationId' should not be null");
        this.exception = Objects.requireNonNull(exception, "'exception' should not be null");
        this.exceptionTypes = Objects.requireNonNull(exceptionTypes, "'exceptionTypes' should not be null");
    }

    /**
     * Sets the topic name of the failed record.
     *
     * @param topic the topic?
     * @return {@code this}
     */
    public Failed withRecordTopic(final String topic) {
        this.topic = Objects.requireNonNull(topic, "'topic' should not be null");
        return this;
    }

    /**
     * Sets the partition of the failed record.
     *
     * @param partition the partition?
     * @return {@code this}
     */
    public Failed withRecordPartition(final int partition) {
        this.partition = partition;
        return this;
    }

    /**
     * Sets the offset of the failed record.
     *
     * @param offset the offset.
     * @return {@code this}
     */
    public Failed withRecordOffset(final long offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Sets the topic name of the failed record.
     *
     * @param recordType the {@link RecordType}.
     * @return {@code this}
     */
    public Failed withRecordType(final RecordType recordType) {
        this.recordType = recordType;
        return this;
    }

    /**
     * Sets the headers to add to the record send to the DLQ.
     *
     * @param customHeaders the headers.
     * @return {@code this}
     */
    public Failed withCustomHeaders(final List<Header> customHeaders) {
        this.customHeaders = customHeaders;
        return this;
    }

    /**
     * @return the stream application id, i.e., {@code application.id}.
     */
    public String applicationId() {
        return applicationId;
    }

    /**
     * @return the {@link ExceptionType}.
     */
    public ExceptionType exceptionTypes() {
        return exceptionTypes;
    }

    /**
     * @return the {@link Exception}.
     */
    public Exception exception() {
        return exception;
    }

    /**
     * @return the record topic.
     */
    public Optional<String> recordTopic() {
        return Optional.ofNullable(topic);
    }

    /**
     * @return the record partition.
     */
    public Optional<Integer> recordPartition() {
        return Optional.ofNullable(partition);
    }

    /**
     * @return the record offset.
     */
    public Optional<Long> recordOffset() {
        return Optional.ofNullable(offset);
    }

    /**
     * @return the record type.
     */
    public Optional<RecordType> recordType() {
        return Optional.ofNullable(recordType);
    }

    /**
     * @return the custom headers.
     */
    public List<Header> customHeaders() {
        return customHeaders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Failed)) return false;
        Failed failed = (Failed) o;
        return Objects.equals(exception, failed.exception) &&
                Objects.equals(applicationId, failed.applicationId) &&
                exceptionTypes == failed.exceptionTypes &&
                Objects.equals(topic, failed.topic) &&
                Objects.equals(partition, failed.partition) &&
                Objects.equals(offset, failed.offset) &&
                recordType == failed.recordType &&
                Objects.equals(customHeaders, failed.customHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                exception,
                applicationId,
                exceptionTypes,
                topic,
                partition,
                offset,
                recordType,
                customHeaders
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Failed{" +
                "exception=" + exception +
                ", applicationId=" + applicationId +
                ", exceptionTypes=" + exceptionTypes +
                ", topic='" + topic +
                ", partition=" + partition +
                ", offset=" + offset +
                ", recordType=" + recordType +
                ", customHeaders=" + customHeaders +
                '}';
    }
}

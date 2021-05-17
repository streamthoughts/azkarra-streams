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

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.utils.Time;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * The default record-headers added to record write to the dead-letter-topic.
 */
public final class ExceptionHeaders {

    public static final String ERROR_EXCEPTION_STACKTRACE = "__errors.exception.stacktrace";
    public static final String ERROR_EXCEPTION_MESSAGE = "__errors.exception.message";
    public static final String ERROR_EXCEPTION_CLASS_NAME = "__errors.exception.class.name";
    public static final String ERROR_TIMESTAMP = "__errors.timestamp";
    public static final String ERROR_APPLICATION_ID = "__errors.application.id";
    public static final String ERROR_RECORD_TOPIC = "__errors.record.topic";
    public static final String ERROR_RECORD_PARTITION = "__errors.record.partition";
    public static final String ERROR_RECORD_OFFSET = "__errors.record.offset";
    public static final String ERROR_RECORD_TOPIC_TYPE = "__errors.record.type";
    public static final String ERROR_TYPE = "__errors.type";

    /**
     * Enrich the given header with the {@link Failed} object.
     *
     * @param headers the {@link Headers} to enrich.
     * @param failed  the {@link Failed} object.
     * @return the {@link Headers}.
     */
    static Headers addExceptionHeaders(final Headers headers, final Failed failed) {
        final Throwable exception = failed.exception();

        Headers enriched = new RecordHeaders(headers);
        enriched.add(ERROR_EXCEPTION_STACKTRACE, toByteArray(getStacktrace(exception)));
        enriched.add(ERROR_EXCEPTION_MESSAGE, toByteArray(exception.getMessage()));
        enriched.add(ERROR_EXCEPTION_CLASS_NAME, toByteArray(exception.getClass().getName()));
        enriched.add(ERROR_TIMESTAMP, toByteArray(Time.SYSTEM.milliseconds()));
        enriched.add(ERROR_APPLICATION_ID, toByteArray(failed.applicationId()));
        enriched.add(ERROR_TYPE, toByteArray(failed.exceptionTypes().name()));

        failed.recordTopic()
                .ifPresent(it -> enriched.add(ERROR_RECORD_TOPIC, toByteArray(it)));
        failed.recordPartition()
                .ifPresent(it -> enriched.add(ERROR_RECORD_PARTITION, toByteArray(it)));
        failed.recordOffset()
                .ifPresent(it -> enriched.add(ERROR_RECORD_OFFSET, toByteArray(it)));
        failed.recordType()
                .ifPresent(it -> enriched.add(ERROR_RECORD_TOPIC_TYPE, toByteArray(it.name())));
        return enriched;
    }

    /**
     * @param data the given object as byte array.
     */
    static byte[] toByteArray(final Integer data) {
        return Integer.toString(data).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @param data the given object as byte array.
     */
    static byte[] toByteArray(final Long data) {
        return Long.toString(data).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @param data the given object as byte array.
     */
    static byte[] toByteArray(final String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @return the stacktrace representation for the given {@link Throwable}.
     */
    static String getStacktrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}

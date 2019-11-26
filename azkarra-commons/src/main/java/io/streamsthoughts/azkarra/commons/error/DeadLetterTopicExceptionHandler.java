/*
 * Copyright 2019 StreamThoughts.
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
package io.streamsthoughts.azkarra.commons.error;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.internals.ProcessorContextImpl;
import org.apache.kafka.streams.processor.internals.RecordCollector;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link DeserializationExceptionHandler} implementation that write rejected error to an dead-letter-topic.
 */
public class DeadLetterTopicExceptionHandler implements DeserializationExceptionHandler {

    private static final String OUTPUT_TOPIC_DEFAULT_SUFFIX = "-rejected";

    private Serializer<byte[]> serializer = new ByteArraySerializer();

    private StringSerializer stringSerializer;

    private DeadLetterTopicExceptionHandlerConfig config;

    private List<Header> customHeaders;
    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Map<String, ?> configs) {
        config = new DeadLetterTopicExceptionHandlerConfig(configs);

        customHeaders = config.customHeaders()
                .entrySet()
                .stream()
                .map(e -> new RecordHeader(e.getKey(), toByteArray(e.getValue().toString())))
                .collect(Collectors.toList());

        // We use a StringSerializer as a convenient way to serialize headers.
        // This allow user to configure encoding.
        stringSerializer = new StringSerializer();
        stringSerializer.configure(configs, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeserializationHandlerResponse handle(final ProcessorContext context,
                                                 final ConsumerRecord<byte[], byte[]> record,
                                                 final Exception exception) {

        RecordCollector collector = ((ProcessorContextImpl) context).recordCollector();

        Headers headers = record.headers();

        headers.add(ExceptionHeader.STACKTRACE, toByteArray(getStacktrace(exception)));
        headers.add(ExceptionHeader.MESSAGE, toByteArray(exception.getMessage()));
        headers.add(ExceptionHeader.CLASS_NAME, toByteArray(exception.getClass().getName()));
        headers.add(ExceptionHeader.TIMESTAMP, toByteArray(Time.SYSTEM.milliseconds()));

        customHeaders.forEach(headers::add);

        final String outputTopic =  (config.outputTopic() != null) ?
            config.outputTopic() :
            record.topic() + OUTPUT_TOPIC_DEFAULT_SUFFIX;

        collector.send(outputTopic,
            record.key(),
            record.value(),
            headers,
            record.timestamp(),
            serializer,
            serializer, null);

        return getDeserializationHandlerResponse(exception);
    }

    private DeserializationHandlerResponse getDeserializationHandlerResponse(final Exception exception) {
        List<Class<?>> classes = config.getFatalExceptions();
        DeserializationHandlerResponse response = DeserializationHandlerResponse.CONTINUE;

        final Iterator<Class<?>> iterator = classes.iterator();
        while (iterator.hasNext() &&  response.equals(DeserializationHandlerResponse.CONTINUE)) {
            Class<?> cls = iterator.next();
            if (cls.isAssignableFrom(exception.getClass())) {
                response = DeserializationHandlerResponse.FAIL;
            }
        }

        return response;
    }

    private byte[] toByteArray(final Long l) {
        return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(l).array();
    }

    private byte[] toByteArray(final String str) {
        return stringSerializer.serialize(null, str);
    }

    private String getStacktrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
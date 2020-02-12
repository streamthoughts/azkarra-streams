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
package io.streamthoughts.azkarra.commons.error;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.internals.ProcessorContextImpl;
import org.apache.kafka.streams.processor.internals.RecordCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link DeserializationExceptionHandler} implementation that write rejected records to an dead-letter-topic.
 */
public class DeadLetterTopicExceptionHandler implements DeserializationExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DeadLetterTopicExceptionHandler.class);

    private static final String OUTPUT_TOPIC_DEFAULT_SUFFIX = "-rejected";

    private Serializer<byte[]> serializer = new ByteArraySerializer();

    private StringSerializer stringSerializer;

    private DeadLetterTopicExceptionHandlerConfig config;

    private List<Header> customHeaders;

    private String applicationId;

    private Producer<byte[], byte[]> internalProducer;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Map<String, ?> configs) {
        config = new DeadLetterTopicExceptionHandlerConfig(configs);
        applicationId = (String)configs.get(StreamsConfig.APPLICATION_ID_CONFIG);
        customHeaders = config.customHeaders()
                .entrySet()
                .stream()
                .map(e -> new RecordHeader(e.getKey(), toByteArray(e.getValue().toString())))
                .collect(Collectors.toList());

        // We use a StringSerializer as a convenient way to serialize headers.
        // This allow user to configure encoding.
        stringSerializer = new StringSerializer();
        stringSerializer.configure(configs, false);

        Map<String, Object> internalProducerConfig = config.producerConfigs();
        if (!internalProducerConfig.isEmpty()) {
            LOG.info("Initializing internal KafkaProducer for exception handler: {}", getClass().getSimpleName());
            internalProducer = new KafkaProducer<>(internalProducerConfig, serializer, serializer);
        }
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

        headers.add(ExceptionHeader.ERROR_EXCEPTION_STACKTRACE,
            toByteArray(getStacktrace(exception)));
        headers.add(ExceptionHeader.ERROR_EXCEPTION_MESSAGE,
            toByteArray(exception.getMessage()));
        headers.add(ExceptionHeader.ERROR_EXCEPTION_CLASS_NAME,
            toByteArray(exception.getClass().getName()));
        headers.add(ExceptionHeader.ERROR_TIMESTAMP,
            toByteArray(Time.SYSTEM.milliseconds()));
        headers.add(ExceptionHeader.ERROR_APPLICATION_ID,
            toByteArray(applicationId));

        headers.add(ExceptionHeader.ERROR_RECORD_TOPIC, toByteArray(record.topic()));
        headers.add(ExceptionHeader.ERROR_RECORD_PARTITION, toByteArray(record.partition()));
        headers.add(ExceptionHeader.ERROR_RECORD_OFFSET, toByteArray(record.offset()));

        customHeaders.forEach(headers::add);

        final String outputTopic =  (config.outputTopic() != null) ?
            config.outputTopic() :
            record.topic() + OUTPUT_TOPIC_DEFAULT_SUFFIX;

        LOG.debug(
            "Sending rejected record from topic={}, partition={}, offset={} into topic {}",
            record.topic(),
            record.partition(),
            record.offset(),
            outputTopic
        );
        if (internalProducer != null) {
            ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(
                outputTopic,
                null,
                record.timestamp(),
                record.key(),
                record.value(),
                headers
                );
            internalProducer.send(producerRecord, new Callback() {
                @Override
                public void onCompletion(final RecordMetadata metadata, final Exception exception) {
                    LOG.error("Fail to send corrupted record into topic {}. Ignored record.", outputTopic, exception);
                }
            });
        } else {
            collector.send(outputTopic,
                record.key(),
                record.value(),
                headers,
                record.timestamp(),
                serializer,
                serializer, null);
        }

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

    private byte[] toByteArray(final Integer data) {
        return stringSerializer.serialize(null, Integer.toString(data));
    }

    private byte[] toByteArray(final Long data) {
        return stringSerializer.serialize(null, Long.toString(data));
    }

    private byte[] toByteArray(final String data) {
        return stringSerializer.serialize(null, data);
    }

    private String getStacktrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
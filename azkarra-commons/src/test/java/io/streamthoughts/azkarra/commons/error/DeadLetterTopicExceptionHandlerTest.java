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
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.errors.StreamsException;
import org.apache.kafka.streams.processor.StreamPartitioner;
import org.apache.kafka.streams.processor.internals.ProcessorContextImpl;
import org.apache.kafka.streams.processor.internals.RecordCollector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

class DeadLetterTopicExceptionHandlerTest {

    public static final String TEST_ERROR_MEESAGE = "test error";
    private DeadLetterTopicExceptionHandler handler;

    private MockRecordCollector collector;

    private ProcessorContextImpl context;

    @BeforeEach
    public void setUp() {
        handler = new DeadLetterTopicExceptionHandler();
        handler.configure(Collections.emptyMap());

        context = Mockito.mock(ProcessorContextImpl.class);

        collector = new MockRecordCollector();
        Mockito.when(context.recordCollector()).thenReturn(collector);
    }

    @Test
    public void test() {

        StringSerializer serializer = new StringSerializer();

        byte[] key = serializer.serialize(null, "test-key");
        byte[] value = serializer.serialize(null, "test-value");

        ConsumerRecord<byte[], byte[]> consumerRecord = new ConsumerRecord<>(
            "test-topic",
            0,
            1L,
            key,
            value);

        handler.handle(context, consumerRecord,  new StreamsException(TEST_ERROR_MEESAGE));

        Assertions.assertEquals("test-topic-rejected", collector.capturedTopic);
        Assertions.assertEquals(key, collector.capturedKey);
        Assertions.assertEquals(value, collector.capturedValue);

        Assertions.assertEquals(TEST_ERROR_MEESAGE,
            new String(collector.capturedHeaders.lastHeader(ExceptionHeader.MESSAGE).value()));
        Assertions.assertEquals(StreamsException.class.getName(),
            new String(collector.capturedHeaders.lastHeader(ExceptionHeader.CLASS_NAME).value()));
    }

    private static class MockRecordCollector implements RecordCollector {

        public String capturedTopic;
        public Object capturedKey;
        public Object capturedValue;
        public Headers capturedHeaders;

        @Override
        public <K, V> void send(String topic,
                                K key,
                                V value,
                                Headers headers,
                                Integer partition,
                                Long timestamp,
                                Serializer<K> keySerializer,
                                Serializer<V> valueSerializer) {
            this.capturedTopic = topic;
            this.capturedKey = key;
            this.capturedValue = value;
            this.capturedHeaders = headers;
        }

        @Override
        public <K, V> void send(String topic,
                                K key, V value,
                                Headers headers,
                                Long timestamp,
                                Serializer<K> keySerializer,
                                Serializer<V> valueSerializer,
                                StreamPartitioner<? super K, ? super V> partitioner) {
            this.capturedTopic = topic;
            this.capturedKey = key;
            this.capturedValue = value;
            this.capturedHeaders = headers;
        }

        @Override
        public void init(Producer<byte[], byte[]> producer) {

        }

        @Override
        public void flush() {

        }

        @Override
        public void close() {

        }

        @Override
        public Map<TopicPartition, Long> offsets() {
            return null;
        }
    }
}
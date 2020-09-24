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
package io.streamthoughts.azkarra.api.streams.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;


public class MonitorOffsetsConsumerInterceptorTest {

    private static final String TEST_GROUP = "test-group";

    private static final  TopicPartition TOPIC_PARTITION = new TopicPartition("topic", 0);

    private MonitorOffsetsConsumerInterceptor<String, String> interceptor;

    @BeforeEach
    public void setUp() {
        interceptor = new MonitorOffsetsConsumerInterceptor<>();
        interceptor.configure(new HashMap<>(){{
            put(ConsumerConfig.GROUP_ID_CONFIG, TEST_GROUP);
            put(ConsumerConfig.CLIENT_ID_CONFIG, "test-client");
        }});
    }

    @Test
    public void shouldUpdateConsumerGroupStateWhenCallingOnConsume() {
        final ConsumerRecord<String, String> record = new ConsumerRecord<>(
            TOPIC_PARTITION.topic(),
            TOPIC_PARTITION.partition(),
            123L,
            "k",
            "v"
        );
        interceptor.onConsume(new ConsumerRecords<>(Collections.singletonMap(
                TOPIC_PARTITION, Collections.singletonList(record))
            )
        );
        final ConsumerGroupOffsetsState state = GlobalConsumerOffsetsRegistry.getInstance().offsetsFor(TEST_GROUP);
        final ConsumerLogOffsets logOffsets = state.offsets().get(TOPIC_PARTITION).right();
        Assertions.assertEquals(record.offset(), logOffsets.consumedOffset().offset());
        Assertions.assertEquals(record.timestamp(), logOffsets.consumedOffset().timestamp());
    }

    @Test
    public void shouldUpdateConsumerGroupStateWhenCallingOnCommit() {
        interceptor.onCommit(Collections.singletonMap(TOPIC_PARTITION, new OffsetAndMetadata(123L)));
        final ConsumerGroupOffsetsState state = GlobalConsumerOffsetsRegistry.getInstance().offsetsFor(TEST_GROUP);
        final ConsumerLogOffsets logOffsets = state.offsets().get(TOPIC_PARTITION).right();
        Assertions.assertEquals(123L, logOffsets.committedOffset().offset());
    }
}
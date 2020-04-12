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
package io.streamthoughts.azkarra.api.streams.consumer;

import io.streamthoughts.azkarra.api.monad.Tuple;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumerGroupOffsetsStateTest {

    private static final String TEST_GROUP = "test-group";

    @Test
    public void shouldUpdateLogOffsetsWhenCallingUpdate() {

        final ConsumerGroupOffsetsState state = new ConsumerGroupOffsetsState(TEST_GROUP);
        final ConsumerThreadKey key = new ConsumerThreadKey("t", "c");
        final TopicPartition tp = new TopicPartition("topic", 0);

        final OffsetAndTimestamp ot = new OffsetAndTimestamp(123L);
        state.update(tp, key, log -> log.consumedOffset(ot));
        Tuple<ConsumerThreadKey, ConsumerLogOffsets> tuple = state.offsets().get(tp);
        assertEquals(tuple.left(), key);
        assertEquals(tuple.right().consumedOffset(), ot);
    }

    @Test
    public void shouldGetConsumerGroupOffsetsWhenCallingSnapshot() {
        final ConsumerGroupOffsetsState state = new ConsumerGroupOffsetsState("test-group");
        ConsumerGroupOffsets snapshot = state.snapshot();
        assertNotNull(snapshot);
        assertEquals(TEST_GROUP, snapshot.group());
        assertTrue(snapshot.consumers().isEmpty());
    }
}
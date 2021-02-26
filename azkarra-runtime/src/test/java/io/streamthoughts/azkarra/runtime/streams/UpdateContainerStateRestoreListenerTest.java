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
package io.streamthoughts.azkarra.runtime.streams;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UpdateContainerStateRestoreListenerTest {

    private LocalKafkaStreamsContainer container;

    @BeforeEach
    public void setUp() {
        container = mock(LocalKafkaStreamsContainer.class);
    }

    @Test
    public void should_only_update_once_all_topics_are_completed() {
        UpdateContainerStateRestoreListener listener = new UpdateContainerStateRestoreListener();
        listener.setKafkaStreamsContainer(container);

        listener.onRestoreStart(new TopicPartition("A", 0), "store", 0, 0);
        listener.onRestoreStart(new TopicPartition("A", 1), "store", 0, 0);
        listener.onRestoreEnd(new TopicPartition("A", 0), "store", 0);
        listener.onRestoreEnd(new TopicPartition("A", 1), "store", 0);

        verify(container, times(1))
                .setState(UpdateContainerStateRestoreListener.RestoreListenerState.STATE_RESTORE_START);
        verify(container, times(1))
                .setState(UpdateContainerStateRestoreListener.RestoreListenerState.STATE_RESTORE_COMPLETED);
    }
}
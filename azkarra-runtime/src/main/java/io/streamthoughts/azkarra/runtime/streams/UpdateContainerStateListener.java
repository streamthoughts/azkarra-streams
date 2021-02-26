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

import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainerAware;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.streams.StateChangeEvent;
import io.streamthoughts.azkarra.api.time.Time;
import org.apache.kafka.streams.KafkaStreams;

final class UpdateContainerStateListener implements KafkaStreams.StateListener, KafkaStreamsContainerAware {

    private LocalKafkaStreamsContainer container;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChange(final KafkaStreams.State newState, final KafkaStreams.State oldState) {
        final StateChangeEvent event = new StateChangeEvent(
                Time.SYSTEM.milliseconds(),
                State.Standards.valueOf(newState.name()),
                State.Standards.valueOf(oldState.name())
        );
        container.stateChanges(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKafkaStreamsContainer(final KafkaStreamsContainer container) {
        this.container = (LocalKafkaStreamsContainer) container;
    }
}

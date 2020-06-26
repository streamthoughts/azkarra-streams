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
package io.streamthoughts.azkarra.api.streams;

import org.apache.kafka.streams.KafkaStreams;

/**
 * The status for a {@link KafkaStreams} instance.
 *
 * @see KafkaStreams.State
 * @see KafkaStreamsContainer
 */
public enum State {

    /**
     * The {@link KafkaStreams} instance has been created yet.
     */
    NOT_CREATED,

    /**
     * The {@link KafkaStreams} instance has been created successfully.
     */
    CREATED,

    /**
     * The {@link KafkaStreamsContainer} is waiting for sources
     * topics to be created before starting {@link KafkaStreams} instance.
     */
    WAITING_FOR_TOPICS,

    /**
     * @see KafkaStreams.State#REBALANCING
     */
    REBALANCING,

    /**
     * @see KafkaStreams.State#RUNNING
     */
    RUNNING,

    /**
     * @see KafkaStreams.State#PENDING_SHUTDOWN
     */
    PENDING_SHUTDOWN,

    /**
     * @see KafkaStreams.State#NOT_RUNNING
     */
    NOT_RUNNING,

    /**
     * The {@link KafkaStreamsContainer} shutdown is complete.
     */
    STOPPED,

    /**
     * @see KafkaStreams.State#ERROR
     */
    ERROR

}

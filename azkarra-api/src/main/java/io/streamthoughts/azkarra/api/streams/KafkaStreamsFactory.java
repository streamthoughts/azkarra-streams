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
package io.streamthoughts.azkarra.api.streams;

import io.streamthoughts.azkarra.api.config.Conf;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;

/**
 * The interface which is used for creating new {@link KafkaStreams} instance.
 */
@FunctionalInterface
public interface KafkaStreamsFactory {

    KafkaStreamsFactory DEFAULT = (topology, config) -> new KafkaStreams(topology, config.getConfAsProperties());

    /**
     * Creates a new {@link KafkaStreams} instance for the given topology and config.
     *
     * @param topology          the {@link Topology} instance.
     * @param streamsConfig     the streams configuration.
     * @return                  the {@link KafkaStreams} instance.
     */
    KafkaStreams make(final Topology topology, final Conf streamsConfig);
}

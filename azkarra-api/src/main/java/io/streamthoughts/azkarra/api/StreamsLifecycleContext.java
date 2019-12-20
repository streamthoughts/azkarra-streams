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
package io.streamthoughts.azkarra.api;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.State;
import org.apache.kafka.streams.TopologyDescription;

/**
 * Context about the current {@link org.apache.kafka.streams.KafkaStreams} instance.
 */
public interface StreamsLifecycleContext {

    /**
     * Gets the application identifier of the current streams instance.
     *
     * @return  the application id.
     */
    String getApplicationId();

    /**
     * Gets the topology description of the current streams instance.
     *
     * @return  the {@link TopologyDescription} instance.
     */
    TopologyDescription getTopology();

    /**
     * Gets the configuration of the current streams instance.
     *
     * @return  the {@link Conf} instance.
     */
    Conf getStreamConfig();

    /**
     * Gets the state of the current streams instance.
     *
     * @return  the {@link State}.
     */
    State getState();

    /**
     * Sets the state of the current streams instance.
     *
     * @param state the new {@link State}.
     */
    void setState(final State state);
}

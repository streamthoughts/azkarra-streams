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

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;

/**
 * Class for building {@link org.apache.kafka.streams.StreamsConfig#APPLICATION_ID_CONFIG}.
 */
public interface ApplicationIdBuilder {

    /**
     * Builds the {@link org.apache.kafka.streams.StreamsConfig#APPLICATION_ID_CONFIG} for
     * the specified topology and configuration.
     *
     * @param metadata  the {@link TopologyMetadata} instance.
     *
     * @return a new {@link ApplicationId} instance.
     */
    ApplicationId buildApplicationId(final TopologyMetadata metadata, final Conf streamsConfig);
}

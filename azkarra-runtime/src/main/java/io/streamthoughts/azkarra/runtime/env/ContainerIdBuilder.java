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
package io.streamthoughts.azkarra.runtime.env;

import io.streamthoughts.azkarra.api.ApplicationId;
import io.streamthoughts.azkarra.api.ContainerId;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;

/**
 * A {@code ContainerIdBuilder} is used to build {@link ContainerId}.
 */
public interface ContainerIdBuilder {

    /**
     * Builds the identifier that will be used for identifying the {@link KafkaStreamsContainer} running the topology.
     *
     * @param metadata  the topology's metadata.
     * @param config    the topology's configuration.
     *
     * @return a new {@link ContainerId} instance.
     */
    ContainerId buildContainerId(final ApplicationId applicationId,
                                 final TopologyMetadata metadata,
                                 final Conf config);
}

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
package io.streamthoughts.azkarra.runtime.env.internal;

import io.streamthoughts.azkarra.api.ApplicationId;
import io.streamthoughts.azkarra.api.ContainerId;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.runtime.env.ContainerIdBuilder;

public class DefaultContainerIdBuilder implements ContainerIdBuilder {

    public static final String CONTAINER_ID_CONFIG = "container.id";

    /**
     * {@inheritDoc}
     */
    @Override
    public ContainerId buildContainerId(final ApplicationId applicationId,
                                        final TopologyMetadata metadata,
                                        final Conf config) {
        if (config.hasPath(CONTAINER_ID_CONFIG))
            return new BasicContainerId(config.getString(CONTAINER_ID_CONFIG));

        return BasicContainerId.create(applicationId);
    }
}

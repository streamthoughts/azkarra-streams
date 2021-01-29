/*
 * Copyright 2019-2021 StreamThoughts.
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
package io.streamthoughts.azkarra.streams;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

/**
 * The {@code AutoStart} can be used to add all known topologies into a
 * target {@link StreamsExecutionEnvironment}.
 */
class AutoStart {

    private static final Logger LOG = LoggerFactory.getLogger(AutoStart.class);

    private final boolean enable;
    private final String environment;

    /**
     * Creates a new {@link AutoStart} instance.
     *
     * @param enable        is all topologies should be started automatically.
     * @param environment   the environment that should be used to topologies.
     */
    AutoStart(final boolean enable, final String environment) {
        this.enable = enable;
        this.environment = environment;
    }

    public void runIfEnable(final AzkarraContext context) {
        if (enable) {
            String target = findDefaultExecutionEnvironmentOrThrow(context);
            LOG.info("Auto register all Kafka Streams topologies into environment '{}'", target);
            final Set<TopologyDescriptor> topologies = context.getTopologyDescriptors(target);
            for (TopologyDescriptor desc : topologies) {
                context.addTopology(
                    desc.className(),
                    desc.version().toString(),
                    environment,
                    new InternalExecuted()
                );
            }
        }
    }

    private String findDefaultExecutionEnvironmentOrThrow(final AzkarraContext context) {
        return Optional.ofNullable(environment)
            .or(() -> Optional.ofNullable(context.getDefaultEnvironment()).map(StreamsExecutionEnvironment::name))
            .orElseThrow(() -> new AzkarraException(
                "Failed to find a default environment for auto-starting registered Kafka Streams topologies.")
            );
    }
}

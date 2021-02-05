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

import io.streamthoughts.azkarra.api.providers.Provider;
import io.streamthoughts.azkarra.api.util.Version;
import org.apache.kafka.streams.Topology;

import java.util.Objects;

/**
 * The default interface to supply a Kafka Streams {@link Topology} instance.
 */
public interface TopologyProvider extends Provider<Topology> {

    /**
     * Returns the version for this {@link Topology}.
     *
     * @return  the string version.
     */
    @Override
    String version();

    /**
     * Supplies a new Kafka Streams {@link Topology} instance.
     *
     * @return  the {@link Topology} instance.
     */
    Topology topology();

    /**
     * An helper method to create a static {@link TopologyProvider} returning the provided {@link Topology}.
     *
     * @param topology  the {@link Topology} instance.
     * @param version   the topology's version.
     * @return          a new {@link TopologyProvider} instance.
     */
    static TopologyProvider of(final Topology topology, final Version version) {
        Objects.requireNonNull(topology, "topology should not be null");
        Objects.requireNonNull(version, "version should not be null");
        return new TopologyProvider() {
            @Override
            public String version() {
                return version.toString();
            }

            @Override
            public Topology topology() {
                return topology;
            }
        };
    }
}

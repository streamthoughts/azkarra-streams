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
package io.streamthoughts.azkarra.streams.config.loader.internal;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import org.apache.kafka.streams.KafkaStreams;

import java.util.Objects;
import java.util.Optional;

/**
 * Class which is used to configure a {@link TopologyProvider} instance.
 */
public class TopologyConfig {

    public static final String TOPOLOGY_NAME_CONFIG               = "name";
    public static final String TOPOLOGY_DESCRIPTION_CONFIG        = "description";
    public static final String TOPOLOGY_PROVIDER_ALIAS_CONFIG     = "topology";
    public static final String TOPOLOGY_PROVIDER_VERSION_CONFIG   = "version";
    public static final String TOPOLOGY_STREAMS_CONFIG            = "config";

    /**
     * Static helper that can be used to creates a new {@link TopologyConfig} instance
     * from the provided {@link Conf}.
     *
     * @return a new {@link TopologyConfig} instance.
     */
    public static TopologyConfig read(final Conf conf) {
        return new Reader().read(conf);
    }

    private final String name;
    private final String description;
    private final String version;
    private final String type;
    private final Conf config;

    /**
     * Creates a new {@link TopologyConfig} instance.
     *
     * @param type          the {@link TopologyProvider} class name or alias.
     * @param name          the streams name; can be {@code null}.
     * @param description   the streams description; can be {@code null}.
     * @param version       the {@link TopologyProvider} version; can be {@code null}.
     * @param config        the {@link Conf} instance to be used for configuring the
     *                      {@link TopologyProvider} and {@link KafkaStreams} instance; can be {@code null}.
     */
    private TopologyConfig(final String type,
                           final String name,
                           final String description,
                           final String version,
                           final Conf config) {
        Objects.requireNonNull(type, "type cannot be null");
        this.name = name;
        this.description = description;
        this.type = type;
        this.version = version;
        this.config = config;
    }

    /**
     * Gets the {@link TopologyProvider} class of alias to be used.
     *
     * @return the type alias. Can return {@code null} of the provider is defined using a class name.
     */
    public String type() {
        return type;
    }

    /**
     * Gets the user-defined topology name.
     *
     * @return  the string name.
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Gets the user-defined topology description.
     *
     * @return  an optional description.
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Gets the {@link TopologyProvider} version to be used.
     *
     * @return  an optional version.
     */
    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Gets the {@link Conf} to be used for configuring the
     * {@link TopologyProvider}  and  {@link KafkaStreams} instance.
     *
     * @return  an optional of {@link Conf}.
     */
    public Optional<Conf> config() {
        return Optional.ofNullable(config);
    }


    public static final class Reader {

        public TopologyConfig read(final Conf conf) {
            Objects.requireNonNull(conf, "conf cannot be null");

            final String type = conf.getString(TOPOLOGY_PROVIDER_ALIAS_CONFIG);

            final Conf config = conf.hasPath(TOPOLOGY_STREAMS_CONFIG) ?
                conf.getSubConf(TOPOLOGY_STREAMS_CONFIG) : Conf.empty();

            return new TopologyConfig(
                type,
                conf.getOptionalString(TOPOLOGY_NAME_CONFIG).orElse(null),
                conf.getOptionalString(TOPOLOGY_DESCRIPTION_CONFIG).orElse(null),
                conf.getOptionalString(TOPOLOGY_PROVIDER_VERSION_CONFIG).orElse(null),
                config
            );
        }
    }
}

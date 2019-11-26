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
package io.streamthoughts.azkarra.streams.context.internal;

import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class which is used to configure a {@link StreamsExecutionEnvironment} instance.
 */
public class EnvironmentConfig {

    public final static String ENVIRONMENT_CONFIG           = "config";
    public final static String ENVIRONMENT_NAME_CONFIG      = "name";
    public static final String ENVIRONMENT_STREAMS_CONFIG   = "jobs";

    /**
     * Static helper that can be used to creates a new {@link EnvironmentConfig} instance
     * from the provided {@link Conf}.
     *
     * @return a new {@link EnvironmentConfig} instance.
     */
    public static EnvironmentConfig read(final Conf conf) {
        Objects.requireNonNull(conf, "conf cannot be null");
        return new Reader().read(conf);
    }

    private final String name;
    private final Conf config;
    private final List<TopologyConfig> topologyStreamSettings;

    /**
     * Creates a new {@link }
     *
     * @param name              the environment name.
     * @param topologyConfigs   the environment topologies.
     * @param config            the environment configuration.
     */
    private EnvironmentConfig(final String name,
                              final List<TopologyConfig> topologyConfigs,
                              final Conf config) {
        Objects.requireNonNull(name, "name cannot be null");
        this.name = name;
        this.config = config;
        this.topologyStreamSettings = topologyConfigs;
    }

    /**
     * Gets the environment name.
     *
     * @return  a string name.
     */
    public String name() {
        return name;
    }

    /**
     * Gets the configuration for this environment.
     *
     * @return  a {@link Conf} instance.
     */
    public Conf config() {
        return config;
    }

    /**
     * Gets all the topology streams configurations for this environment.
     *
     * @return  a {@link Conf} instance. Can be empty if no streams is defined for this environment.
     */
    public List<TopologyConfig> topologyStreamConfigs() {
        return topologyStreamSettings;
    }

    public static final class Reader {

        EnvironmentConfig read(final Conf conf) {
            return new EnvironmentConfig(
                conf.getString(ENVIRONMENT_NAME_CONFIG),
                mayGetEnvironmentStreams(conf),
                mayGetConfiguredDefaultsConf(conf)
            );
        }

        private List<TopologyConfig> mayGetEnvironmentStreams(final Conf conf) {
            if (conf.hasPath(ENVIRONMENT_STREAMS_CONFIG)) {
                List<Conf> confs = conf.getSubConfList(ENVIRONMENT_STREAMS_CONFIG);
                return confs.stream()
                        .map(TopologyConfig::read)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        private Conf mayGetConfiguredDefaultsConf(final Conf conf) {
            return conf.hasPath(ENVIRONMENT_CONFIG) ?
                conf.getSubConf(ENVIRONMENT_CONFIG) :
                Conf.empty();
        }
    }
}

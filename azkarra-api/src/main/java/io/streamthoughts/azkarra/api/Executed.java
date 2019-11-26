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
import org.apache.kafka.streams.Topology;

import java.util.Objects;

/**
 * Executed class is used to describe a {@link Topology} instance to be executed.
 */
public class Executed {

    protected final String name;
    protected final String description;
    protected final Conf config;

    /**
     * Static helper that can be used to creates a new {@link Executed} instance
     * with the specified streams name.
     *
     * @param name the name of the streams application.
     *
     * @return a new {@link StreamsExecutionEnvironment} instance.
     */
    public static Executed as(final String name) {
        return new Executed(name, null, null );
    }

    /**
     * Static helper that can be used to creates a new {@link Executed} instance
     * with the specified streams name and description.
     *
     * @param name        the name of the streams application.
     * @param description the description of the streams application.
     *
     * @return a new {@link StreamsExecutionEnvironment} instance.
     */
    public static Executed as(final String name, final String description) {
        return new Executed(name, description, null);
    }

    /**
     * Static helper that can be used to creates a new {@link Executed} instance
     * with the specified streams name and configuration.
     *
     * @param conf the configuration.
     *
     * @return a new {@link StreamsExecutionEnvironment} instance.
     */
    public static Executed with(final Conf conf) {
        return new Executed(null, null, conf);
    }

    /**
     * Creates a new {@link Executed} instance.
     */
    protected Executed() {
        this(null, null, null);
    }

    /**
     * Creates a new {@link Executed} instance.
     *
     * @param name              the name to be used for the streams application.
     * @param description       the description to be used for the streams application.
     * @param config            the {@link Conf} to be used
     *                          for configuring the {@link Topology} the {@link org.apache.kafka.streams.KafkaStreams}.
     */
    private Executed(final String name,
                     final String description,
                     final Conf config) {
        this.name = name;
        this.description = description;
        this.config = config;
    }


    protected Executed(final Executed executed) {
        this(
            executed.name,
            executed.description,
            executed.config
        );
    }

    public Executed withName(final String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return new Executed(
            name,
            description,
            config
        );
    }

    public Executed withDescription(final String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return new Executed(
            name,
            description,
            config
        );
    }

    public Executed withConfig(final Conf config) {
        Objects.requireNonNull(config, "config cannot be null");
        return new Executed(
            name,
            description,
            config
        );
    }
}

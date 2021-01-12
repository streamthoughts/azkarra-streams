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

package io.streamthoughts.azkarra.api;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.util.Version;

import java.util.Objects;

public class StreamsTopologyMeta {

    private final String name;
    private final Version version;
    private final String description;
    private final Class<TopologyProvider> type;
    private final ClassLoader classLoader;
    private final Conf conf;


    /**
     * Creates a new {@link StreamsTopologyMeta} instance.
     *
     * @param name          the name of the topology.
     * @param version       the version of the topology
     * @param description   the description of the topology.
     * @param type          the topology {@link Class}.
     * @param classLoader   the topology {@link ClassLoader}.
     * @param conf          the default {@link Conf} for the topology.
     */
    public StreamsTopologyMeta(final String name,
                               final Version version,
                               final String description,
                               final Class<TopologyProvider> type,
                               final ClassLoader classLoader,
                               final Conf conf) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.type = type;
        this.classLoader = classLoader;
        this.conf = conf;
    }

    public String name() {
        return name;
    }

    public Version version() {
        return version;
    }

    public Class<TopologyProvider> type() {
        return type;
    }

    public String description() {
        return description;
    }

    public Conf configuration() {
        return conf;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StreamsTopologyMeta)) return false;
        StreamsTopologyMeta that = (StreamsTopologyMeta) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(description, that.description) &&
                Objects.equals(type, that.type) &&
                Objects.equals(classLoader, that.classLoader) &&
                Objects.equals(conf, that.conf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, version, description, type, classLoader, conf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "StreamTopologyMeta{" +
                "name='" + name + '\'' +
                ", version=" + version +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", classLoader=" + classLoader +
                ", conf=" + conf +
                '}';
    }
}

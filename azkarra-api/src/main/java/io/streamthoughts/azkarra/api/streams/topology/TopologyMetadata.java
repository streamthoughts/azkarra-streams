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
package io.streamthoughts.azkarra.api.streams.topology;

import java.util.Objects;

public class TopologyMetadata {

    private final String name;
    private final String version;
    private final String description;

    /**
     * Creates a new {@link TopologyMetadata} instance.
     *
     * @param name              the topology name.
     * @param version           the topology version.
     * @param description       the topology description.
     */
    public TopologyMetadata(final String name,
                            final String version,
                            final String description) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(version, "version cannot be null");

        if (name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }

        this.name = name;
        this.version = version;
        this.description = description;
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
    }

    public String description() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopologyMetadata)) return false;
        TopologyMetadata metadata = (TopologyMetadata) o;
        return Objects.equals(name, metadata.name) &&
                Objects.equals(version, metadata.version) &&
                Objects.equals(description, metadata.description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, version, description);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "TopologyMetadata{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

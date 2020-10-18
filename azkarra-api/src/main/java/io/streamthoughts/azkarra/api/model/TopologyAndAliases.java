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

package io.streamthoughts.azkarra.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Set;

public class TopologyAndAliases {

    private String type;
    private Set<String> aliases;

    public TopologyAndAliases(final String type,
                              final Set<String> aliases) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.aliases = Objects.requireNonNull(aliases, "aliases cannot be null");;
    }

    @JsonProperty("type")
    public String type() {
        return type;
    }

    @JsonProperty("aliases")
    public Set<String> aliases() {
        return aliases;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopologyAndAliases)) return false;
        TopologyAndAliases that = (TopologyAndAliases) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(aliases, that.aliases);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, aliases);
    }
}

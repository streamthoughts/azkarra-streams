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
package io.streamthoughts.azkarra.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class Metric {

    private final String name;
    private final String group;
    private final String description;
    private final Map<String, String> tags;
    private final Object value;

    private int hash = 0;

    public Metric(final String name,
                  final String group,
                  final String description,
                  final Map<String, String> tags,
                  final Object value) {
        this.name = name;
        this.group = group;
        this.description = description;
        this.tags = tags;
        this.value = value;
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    @JsonProperty("description")
    public String description() {
        return description;
    }

    @JsonProperty("tags")
    public Map<String, String> tags() {
        return tags;
    }

    @JsonProperty("value")
    public Object value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Metric)) return false;
        Metric metric = (Metric) o;
        return Objects.equals(name, metric.name) &&
               Objects.equals(group, metric.group) &&
               Objects.equals(description, metric.description) &&
               Objects.equals(tags, metric.tags);

    }

    @Override
    public int hashCode() {
        if (hash == 0)
            hash = Objects.hash(name, group, description, tags);
        return hash;
    }

    @Override
    public String toString() {
        return "[" +
                "name=" + name +
                "group=" + group +
                ", description=" + description +
                ", tags=" + tags +
                ", value=" + value +
                ']';
    }
}

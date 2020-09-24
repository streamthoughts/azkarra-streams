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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MetricGroup implements Comparable<MetricGroup> {

    private final String name;

    private final List<Metric> metrics;

    /**
     * Creates a new {@link MetricGroup} instance.
     * @param name  the group name.
     */
    public MetricGroup(final String name, final List<Metric> metrics) {
        Objects.requireNonNull(name, "name cannot be null");
        this.name = name;
        this.metrics = metrics;
        metrics.sort(Comparator.comparing(Metric::name));
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    @JsonProperty("metrics")
    public List<Metric> metrics() {
        return metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetricGroup)) return false;
        MetricGroup that = (MetricGroup) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(metrics, that.metrics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, metrics);
    }

    @Override
    public String toString() {
        return "[" +
                "name=" + name +
                ", metrics=" + metrics +
                ']';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final MetricGroup that) {
        return this.name.compareTo(that.name);
    }
}

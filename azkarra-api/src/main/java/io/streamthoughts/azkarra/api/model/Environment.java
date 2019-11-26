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
import io.streamthoughts.azkarra.api.State;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Environment {

    private final String name;
    private final State state;
    private final Map<String, Object> config;
    private final Set<String> applications;
    private final boolean isDefault;

    /**
     * Creates a new {@link Environment} instance.
     *
     * @param name              the environment name.
     * @param state             the environment state.
     * @param config            the environment configuration.
     * @param applications      the list of active streams applications.
     * @param isDefault         is the default environment.
     */
    public Environment(final String name,
                       final State state,
                       final Map<String, Object> config,
                       final Set<String> applications,
                       final boolean isDefault) {
        this.name = name;
        this.state = state;
        this.config = config;
        this.applications = new TreeSet<>(applications);
        this.isDefault = isDefault;
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    @JsonProperty("state")
    public State state() {
        return state;
    }

    @JsonProperty("config")
    public Map<String, Object> config() {
        return new TreeMap<>(config);
    }

    @JsonProperty("applications")
    public Set<String> applications() {
        return applications;
    }

    public boolean isDefault() {
        return isDefault;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Environment)) return false;
        Environment that = (Environment) o;
        return isDefault == that.isDefault &&
                Objects.equals(name, that.name) &&
                Objects.equals(config, that.config) &&
                Objects.equals(applications, that.applications);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, config, applications, isDefault);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Environment{" +
                "name='" + name + '\'' +
                ", config=" + config +
                ", applications=" + applications +
                ", isDefault=" + isDefault +
                '}';
    }
}

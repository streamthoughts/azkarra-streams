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
package io.streamthoughts.azkarra.api.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Helper class which can be used for building new {@link Conf} instance.
 */
public class ConfBuilder implements Conf {

    private final Map<String, Object> parameters;

    public static ConfBuilder newConf(final Conf conf) {
        return new ConfBuilder(conf.getConfAsMap());
    }

    public static ConfBuilder newConf() {
        return new ConfBuilder(new HashMap<>());
    }

    private ConfBuilder(final Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public ConfBuilder with(final String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(final String path) {
        return build().getString(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(final String path) {
        return build().getLong(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(final String path) {
        return build().getInt(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean(final String path) {
        return build().getBoolean(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(final String path) {
        return build().getDouble(path);
    }

    @Override
    public List<String> getStringList(final String path) {
        return build().getStringList(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getSubConf(final String path) {
        return build().getSubConf(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Conf> getSubConfList(final String path) {
        return build().getSubConfList(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPath(final String path) {
        return build().hasPath(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf withFallback(final Conf fallback) {
        return build().withFallback(fallback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getClasses(final String path, final Class<T> type) {
        return build().getClasses(path, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getClass(final String path, final Class<T> type) {
        return build().getClass(path, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfAsMap() {
        return build().getConfAsMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getConfAsProperties() {
        return build().getConfAsProperties();
    }

    public Conf build() {
        return Conf.with(parameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return parameters.toString();
    }
}

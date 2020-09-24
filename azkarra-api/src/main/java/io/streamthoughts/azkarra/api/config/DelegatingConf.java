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
package io.streamthoughts.azkarra.api.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Delegates to a {@link Conf}.
 */
public class DelegatingConf implements Conf  {

    protected final Conf originals;

    /**
     * Creates a new {@link DelegatingConf} instance.
     *
     * @param originals the originals {@link Conf}.
     */
    public DelegatingConf(final Conf originals) {
        this.originals = originals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(final String path) {
        return originals.getString(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(final String path) {
        return originals.getLong(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(final String path) {
        return originals.getInt(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean(final String path) {
        return originals.getBoolean(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(final String path) {
        return originals.getDouble(path);
    }

    @Override
    public List<String> getStringList(final String path) {
        return originals.getStringList(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getSubConf(final String path) {
        return originals.getSubConf(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Conf> getSubConfList(final String path) {
        return originals.getSubConfList(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPath(final String path) {
        return originals.hasPath(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf withFallback(final Conf fallback) {
        return originals.withFallback(fallback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getClasses(final String path, final Class<T> type) {
        return originals.getClasses(path, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getClass(final String path, final Class<T> type) {
        return originals.getClass(path, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfAsMap() {
        return originals.getConfAsMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getConfAsProperties() {
        return originals.getConfAsProperties();
    }
}

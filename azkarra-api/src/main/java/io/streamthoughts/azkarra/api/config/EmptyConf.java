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

import io.streamthoughts.azkarra.api.errors.MissingConfException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class EmptyConf implements Conf {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(final String path) {
        throw new MissingConfException(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(final String key) {
        throw new MissingConfException(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(final String key) {
        throw new MissingConfException(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean(final String key) {
        throw new MissingConfException(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(final String key) {
        throw new MissingConfException(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getStringList(final String key) {
        throw new MissingConfException(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getSubConf(final String key) {
        throw new MissingConfException(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Conf> getSubConfList(final String key) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPath(final String key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf withFallback(final Conf fallback) {
        return fallback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getClasses(final String key, final Class<T> type) {
        throw new MissingConfException(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getClass(final String key, final Class<T> type) {
        throw new MissingConfException(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfAsMap() {
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getConfAsProperties() {
        return new Properties();
    }
}

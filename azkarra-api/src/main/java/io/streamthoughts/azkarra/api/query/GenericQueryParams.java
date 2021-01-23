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
package io.streamthoughts.azkarra.api.query;

import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GenericQueryParams implements QueryParams {

    public static GenericQueryParams empty() {
        return new GenericQueryParams(Collections.emptyMap());
    }

    private final Map<String, Object> parameters;

    /**
     * Creates a new {@link GenericQueryParams} instance.
     */
    public GenericQueryParams() {
        this(new HashMap<>());
    }

    /**
     * Creates a new {@link GenericQueryParams} instance.
     *
     * @param params the key-value parameters.
     */
    public GenericQueryParams(final Map<String, Object> params) {
        this.parameters = new HashMap<>(params);
    }

    public GenericQueryParams put(final String key, final Object object) {
        parameters.put(key, object);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(final String key) {
        return (V) parameters.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(final String key) {
        return parameters.get(key).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getLong(final String key) {
        return Conf.of(key, parameters.get(key)).getLong(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(final String key) {
        return parameters.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getAsMap() {
        return new HashMap<>(parameters);
    }
}


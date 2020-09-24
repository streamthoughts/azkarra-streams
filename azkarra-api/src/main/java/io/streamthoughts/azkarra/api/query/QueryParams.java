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

import io.streamthoughts.azkarra.api.config.MapConf;

import java.util.Collections;
import java.util.Map;

public class QueryParams {

    public static QueryParams empty() {
        return new QueryParams(Collections.emptyMap());
    }

    private final InnerMapConf parameters;

    /**
     * Creates a new {@link QueryParams} instance.
     *
     * @param params the key-value parameters.
     */
    public QueryParams(final Map<String, Object> params) {
        this.parameters = new InnerMapConf(params);
    }

    /**
     * Gets the parameter for the given key.
     *
     * @param key   the parameter key.
     * @return      the object value.
     */
    @SuppressWarnings("unchecked")
    public <V> V getValue(final String key) {
        return (V) originals().get(key);
    }

    /**
     * Gets the parameter as string for the given key.
     *
     * @param key   the parameter key.
     * @return      the string value.
     */
    public String getString(final String key) {
        return parameters.getString(key);
    }

    /**
     * Gets the parameter as long for the given key.
     *
     * @param key   the parameter key.
     * @return      the long value.
     */
    public Long getLong(final String key) {
        return parameters.getLong(key);
    }

    public boolean contains(final String key) {
        return parameters.hasPath(key);
    }

    public Map<String, Object> originals() {
        return Collections.unmodifiableMap(parameters.originals());
    }

    @Override
    public String toString() {
        return "Parameters{" +
                "params=" + parameters.originals() +
                '}';
    }

    private static class InnerMapConf extends MapConf {

        InnerMapConf(final Map<String, ?> props) {
            super(props, null, false); // we don't need to explode the Map.
        }

        Map<String, ?> originals() {
            return parameters;
        }
    }
}

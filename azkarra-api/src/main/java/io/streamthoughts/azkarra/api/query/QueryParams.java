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
package io.streamthoughts.azkarra.api.query;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class QueryParams {

    private final Map<String, Object> params;

    public static QueryParams empty() {
        return new QueryParams(Collections.emptyMap());
    }

    /**
     * Creates a new {@link QueryParams} instance.
     *
     * @param params the key-value parameters.
     */
    public QueryParams(final Map<String, Object> params) {
        Objects.requireNonNull(params,"params can't be null");
        this.params = params;
    }

    public boolean contains(final String key) {
        return params.containsKey(key);
    }

    public <T> T getValue(final String key) {
        checkContains(key);
        return (T) params.get(key);
    }

    private void checkContains(final String key) {
        Objects.requireNonNull(key, "key can't be null");
        if (!params.containsKey(key)) {
            throw new IllegalArgumentException("no param for key '" + key + "'");
        }
    }

    public Map<String, Object> originals() {
        return Collections.unmodifiableMap(params);
    }

    @Override
    public String toString() {
        return "Parameters{" +
                "params=" + params +
                '}';
    }
}

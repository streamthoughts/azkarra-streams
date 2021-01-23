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

import java.util.Map;

public interface QueryParams {
    
    /**
     * Gets the parameter for the given key.
     *
     * @param key the parameter key.
     * @return the object value.
     */
    <V> V getValue(final String key);

    /**
     * Gets the parameter as string for the given key.
     *
     * @param key the parameter key.
     * @return the string value.
     */
    String getString(final String key);

    /**
     * Gets the parameter as long for the given key.
     *
     * @param key the parameter key.
     * @return the long value.
     */
    Long getLong(final String key);

    /**
     * Checks whether this {@link QueryParams} contains the given key param.
     *
     * @param key   the key params.
     * @return      {@code true} if this {@link QueryParams} contains the given key.
     */
    boolean contains(String key);

    /**
     * Gets all query params as {@code Map}.
     *
     * @return  a new {@link Map}.
     */
    Map<String, Object> getAsMap();
}

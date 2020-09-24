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
package io.streamthoughts.azkarra.api.query.internal;

import java.util.Objects;

public class QueryBuilder {

    private final String storeName;

    /**
     * Creates a new {@link QueryBuilder} instance.
     *
     * @param storeName the store name to query.
     */
    public QueryBuilder(final String storeName) {
        Objects.requireNonNull(storeName, "storeName cannot be null");
        this.storeName = storeName;
    }

    /**
     * Key-Value store.
     */
    public KeyValueQueryBuilder keyValue() {
        return new KeyValueQueryBuilder(storeName);
    }

    /**
     * Session store.
     */
    public SessionQueryBuilder session() {
        return new SessionQueryBuilder(storeName);
    }

    /**
     * Window store.
     */
    public WindowQueryBuilder window() {
        return new WindowQueryBuilder(storeName);
    }

    /**
     * Timestamped Key-Value store.
     */
    public TimestampedKeyValueQueryBuilder timestampedKeyValue() {
       return new TimestampedKeyValueQueryBuilder(storeName);
    }

    /**
     * Timestamped Window store
     */
    public TimestampedWindowQueryBuilder timestampedWindow() {
        return new TimestampedWindowQueryBuilder(storeName);
    }




}

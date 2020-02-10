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
package io.streamthoughts.azkarra.api.query.internal;

import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.QueryParams;

public class TimestampedKeyValueQueryBuilder extends KeyValueQueryBuilder {

    /**
     * Creates a new {@link TimestampedKeyValueQueryBuilder} instance.
     *
     * @param storeName     the name of the store.
     */
    TimestampedKeyValueQueryBuilder(final String storeName) {
        super(storeName);
    }

    public <K, V> Query<K, V> all() {
        return new Query<>(storeName, (store, parameters) -> new TimestampedKeyValueGetAllQuery<>(store));
    }

    public <K, V> Query<K, V> get() {
        return new Query<>(storeName, new TimestampedGetKeyValueQueryBuilder<>());
    }

    public <K, V> Query<K, V> range() {
        return new Query<>(storeName, new TimestampedGetKeyValueRangeQueryBuilder<>());
    }

    static class TimestampedGetKeyValueQueryBuilder<K, V> extends KeyValueQueryBuilder.GetKeyValueQueryBuilder<K, V> {

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<K, V> build(final String store, final QueryParams parameters) {

            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new TimestampedKeyValueGetQuery<>(store, p.getValue(QUERY_PARAM_KEY), null);
        }
    }

    static class TimestampedGetKeyValueRangeQueryBuilder<K, V> extends GetKeyValueRangeQueryBuilder<K, V>  {

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<K, V>  build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new TimestampedKeyValueGetRangeQuery<>(
                    store,
                    p.getValue(QUERY_PARAM_KEY_FROM),
                    p.getValue(QUERY_PARAM_KEY_TO)
            );
        }
    }
}

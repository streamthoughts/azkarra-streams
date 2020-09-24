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

import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.QueryParams;
import org.apache.kafka.streams.kstream.Windowed;

import java.time.Instant;

public class TimestampedWindowQueryBuilder extends WindowQueryBuilder {

    /**
     * Creates a new {@link TimestampedWindowQueryBuilder} instance.
     * @param storeName         the name of the store.
     */
    TimestampedWindowQueryBuilder(final String storeName) {
        super(storeName);
    }

    public <K, V> Query<K, V> fetch() {
        return new Query<>(storeName, new TimestampedFetchWindowQueryBuilder<>());
    }

    public <K, V> Query<Windowed<K>, V> fetchKeyRange() {
        return new Query<>(storeName, new TimestampedWindowFetKeyRangeQueryBuilder<>());
    }

    public <V> Query<Long, V> fetchTimeRange() {
        return new Query<>(storeName, new TimestampedWindowFetchTimeRangeQueryBuilder<>());
    }

    public <K, V> Query<Windowed<K>, V> fetchAll() {
        return new Query<>(storeName, new TimestampedWindowFetchAllQueryBuilder<>());
    }

    public <K, V> Query<Windowed<K>, V> all() {
        return new Query<>(storeName, (store, parameters) -> new TimestampedWindowGetAllQuery<>(store));
    }

    static class TimestampedFetchWindowQueryBuilder<K, V> extends WindowQueryBuilder.FetchWindowQueryBuilder<K, V> {

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<K, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new TimestampedWindowFetchQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY),
                null,
                p.getLong(QUERY_PARAM_TIME)
            );
        }
    }

    static class TimestampedWindowFetKeyRangeQueryBuilder<K, V> extends
            WindowFetchKeyRangeQueryBuilder<K, V> {

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<Windowed<K>, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new TimestampedWindowFetchKeyRangeQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY_FROM),
                p.getValue(QUERY_PARAM_KEY_TO),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_TO))
            );
        }
    }

    static class TimestampedWindowFetchTimeRangeQueryBuilder<V>
            extends WindowQueryBuilder.WindowFetchTimeRangeQueryBuilder<V> {

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<Long, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new TimestampedWindowFetchTimeRangeQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_TO))
            );
        }
    }

    static class TimestampedWindowFetchAllQueryBuilder<K, V>
            extends WindowQueryBuilder.WindowFetchAllQueryBuilder<K, V> {

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<Windowed<K>, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new TimestampedWindowFetchAllQuery<>(
                store,
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_TO))
            );
        }
    }
}

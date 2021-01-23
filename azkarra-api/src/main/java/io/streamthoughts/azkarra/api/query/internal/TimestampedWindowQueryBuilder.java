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

import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.LocalPreparedQuery;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.error.InvalidQueryException;
import org.apache.kafka.streams.kstream.Windowed;

import java.time.Instant;

import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_FROM;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_TO;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_TIME_FROM;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_TIME_TO;

public class TimestampedWindowQueryBuilder extends WindowQueryBuilder {

    /**
     * Creates a new {@link TimestampedWindowQueryBuilder} instance.
     *
     * @param store     the name of the store.
     */
    TimestampedWindowQueryBuilder(final String store) {
       super(store);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalPreparedQuery<K, V> fetch() {
        return new TimestampedFetchWindowQueryBuilder<>(store);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalPreparedQuery<Windowed<K>, V> fetchKeyRange() {
        return new TimestampedWindowFetKeyRangeQueryBuilder<>(store);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalPreparedQuery<Long, V> fetchTimeRange() {
        return new TimestampedWindowFetchTimeRangeQueryBuilder<>(store);
    }

    public <K, V> LocalPreparedQuery<Windowed<K>, V> fetchAll() {
        return new TimestampedWindowFetchAllQueryBuilder<>(store);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalPreparedQuery<Windowed<K>, V> all() {
        return params -> new TimestampedWindowGetAllQuery<>(store);
    }

    static class TimestampedFetchWindowQueryBuilder<K, V> extends WindowQueryBuilder.FetchWindowQueryBuilder<K, V> {

        public TimestampedFetchWindowQueryBuilder(final String store) {
            super(store);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<K, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new TimestampedWindowFetchQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY),
                null,
                p.getLong(QueryConstants.QUERY_PARAM_TIME)
            );
        }
    }

    static class TimestampedWindowFetKeyRangeQueryBuilder<K, V> extends
            WindowFetchKeyRangeQueryBuilder<K, V> {

        public TimestampedWindowFetKeyRangeQueryBuilder(final String store) {
            super(store);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<Windowed<K>, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new TimestampedWindowFetchKeyRangeQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY_FROM),
                p.getValue(QUERY_PARAM_KEY_TO),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_TO))
            );
        }
    }

    static class TimestampedWindowFetchTimeRangeQueryBuilder<K, V>
            extends WindowQueryBuilder.WindowFetchTimeRangeQueryBuilder<K, V> {

        public TimestampedWindowFetchTimeRangeQueryBuilder(final String store) {
            super(store);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<Long, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
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

        public TimestampedWindowFetchAllQueryBuilder(final String store) {
            super(store);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<Windowed<K>, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new TimestampedWindowFetchAllQuery<>(
                store,
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_TO))
            );
        }
    }
}

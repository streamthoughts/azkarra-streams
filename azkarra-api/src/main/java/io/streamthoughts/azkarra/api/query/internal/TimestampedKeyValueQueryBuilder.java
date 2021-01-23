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

public class TimestampedKeyValueQueryBuilder extends KeyValueQueryBuilder {

    /**
     * Creates a new {@link TimestampedKeyValueQueryBuilder} instance.
     */
    TimestampedKeyValueQueryBuilder(final String store) {
        super(store);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalPreparedQuery<K, V> all() {
        return params -> new TimestampedKeyValueGetAllQuery<>(store);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalPreparedQuery<K, V> get() {
        return new TimestampedGetKeyValueQueryBuilder<>(store);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> LocalPreparedQuery<K, V> range() {
        return new TimestampedGetKeyValueRangeQueryBuilder<>(store);
    }

    static class TimestampedGetKeyValueQueryBuilder<K, V> extends GetKeyValuePreparedQuery<K, V> {

        public TimestampedGetKeyValueQueryBuilder(final String store) {
            super(store);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<K, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new TimestampedKeyValueGetQuery<>(store, p.getValue(QueryConstants.QUERY_PARAM_KEY), null);
        }
    }

    static class TimestampedGetKeyValueRangeQueryBuilder<K, V> extends GetKeyValueRangePreparedQuery<K, V>  {

        public TimestampedGetKeyValueRangeQueryBuilder(final String store) {
            super(store);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<K, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new TimestampedKeyValueGetRangeQuery<>(
                store,
                p.getValue(QueryConstants.QUERY_PARAM_KEY_FROM),
                p.getValue(QueryConstants.QUERY_PARAM_KEY_TO)
            );
        }
    }
}

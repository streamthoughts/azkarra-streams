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

import io.streamthoughts.azkarra.api.monad.Validator;
import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.LocalPreparedQuery;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.error.InvalidQueryException;

import java.util.Objects;

import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_FROM;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_TO;

public class KeyValueQueryBuilder implements QueryOperationBuilder {

    protected final String store;

    /**
     * Creates a new {@link KeyValueQueryBuilder} instance.
     *
     * @param store     the name of the store.
     */
    KeyValueQueryBuilder(final String store) {
        this.store = store;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalPreparedQuery prepare(final StoreOperation operation) {

        if (operation == StoreOperation.GET)
            return get();
        if (operation == StoreOperation.RANGE)
            return range();
        if (operation == StoreOperation.ALL)
            return all();
        if (operation == StoreOperation.COUNT)
            return count();

        throw new InvalidQueryException("Operation not supported '" + operation.name() + "'");
    }

    public <K, V> LocalPreparedQuery<K, V> all() {
        return params -> new KeyValueGetAllQuery<>(store);
    }

    public <K, V> LocalPreparedQuery<K, V> get() {
        return new GetKeyValuePreparedQuery<>(store);
    }

    public <K, V> LocalPreparedQuery<K, V> range() {
        return new GetKeyValueRangePreparedQuery<>(store);
    }

    public LocalPreparedQuery<String, Long> count() {
        return params -> new KeyValueCountQuery(store);
    }

    static class GetKeyValuePreparedQuery<K, V> implements LocalPreparedQuery<K, V> {

        protected final String store;

        public GetKeyValuePreparedQuery(final String store) {
            this.store = Objects.requireNonNull(store, "store should not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validator(final QueryParams params) {
            return Validator
                .of(params)
                .validates(p -> p.contains(QUERY_PARAM_KEY), MissingRequiredKeyError.of(QUERY_PARAM_KEY));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<K, V> compile(final QueryParams params) throws InvalidQueryException {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new KeyValueGetQuery<>(store, p.getValue(QUERY_PARAM_KEY), null);
        }
    }

    static class GetKeyValueRangePreparedQuery<K, V> implements LocalPreparedQuery<K, V> {

        protected final String store;

        public GetKeyValueRangePreparedQuery(String store) {
            this.store = Objects.requireNonNull(store, "store should not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validator(final QueryParams parameters) {
            return Validator.of(parameters)
                    .validates(p -> p.contains(QUERY_PARAM_KEY_FROM), MissingRequiredKeyError.of(QUERY_PARAM_KEY_FROM))
                    .validates(p -> p.contains(QUERY_PARAM_KEY_TO), MissingRequiredKeyError.of(QUERY_PARAM_KEY_TO));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<K, V>  compile(final QueryParams params) throws InvalidQueryException {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new KeyValueGetRangeQuery<>(store, p.getValue(QUERY_PARAM_KEY_FROM), p.getValue(QUERY_PARAM_KEY_TO));
        }
    }
}

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

import io.streamthoughts.azkarra.api.monad.Validator;
import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.StoreOperation;

import java.util.Objects;

public class KeyValueQueryBuilder implements QueryOperationBuilder {

    public static final String QUERY_PARAM_KEY = "key";
    public static final String QUERY_PARAM_KEY_FROM = "keyFrom";
    public static final String QUERY_PARAM_KEY_TO = "keyTo";

    protected final String storeName;

    /**
     * Creates a new {@link KeyValueQueryBuilder} instance.
     * @param storeName     the name of the store.
     */
    KeyValueQueryBuilder(final String storeName) {
        Objects.requireNonNull(storeName, "storeName cannot be null");
        this.storeName = storeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query operation(final StoreOperation operation) {

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

    public <K, V> Query<K, V> all() {
        return new Query<>(storeName, (store, parameters) -> new KeyValueGetAllQuery<>(store));
    }

    public <K, V> Query<K, V> get() {
        return new Query<>(storeName, new GetKeyValueQueryBuilder<>());
    }

    public <K, V> Query<K, V> range() {
        return new Query<>(storeName, new GetKeyValueRangeQueryBuilder<>());
    }

    public Query<String, Long> count() {
        return new Query<>(storeName, (store, parameters) -> new KeyValueCountQuery(store));
    }

    static class GetKeyValueQueryBuilder<K, V> implements LocalStoreQueryBuilder<K, V>  {

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validates(final QueryParams parameters) {
            return Validator.of(parameters)
                    .validates(p -> p.contains(QUERY_PARAM_KEY), MissingRequiredKeyError.of(QUERY_PARAM_KEY));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<K, V> build(final String store, final QueryParams parameters) {

            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new KeyValueGetQuery<>(store, p.getValue(QUERY_PARAM_KEY), null);
        }
    }

    static class GetKeyValueRangeQueryBuilder<K, V> implements LocalStoreQueryBuilder<K, V>  {

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validates(final QueryParams parameters) {
            return Validator.of(parameters)
                    .validates(p -> p.contains(QUERY_PARAM_KEY_FROM), MissingRequiredKeyError.of(QUERY_PARAM_KEY_FROM))
                    .validates(p -> p.contains(QUERY_PARAM_KEY_TO), MissingRequiredKeyError.of(QUERY_PARAM_KEY_TO));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<K, V>  build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new KeyValueGetRangeQuery<>(
                    store,
                    p.getValue(QUERY_PARAM_KEY_FROM),
                    p.getValue(QUERY_PARAM_KEY_TO)
            );
        }
    }
}

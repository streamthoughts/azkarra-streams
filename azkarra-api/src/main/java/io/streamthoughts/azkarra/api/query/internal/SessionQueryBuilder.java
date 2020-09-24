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
import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import org.apache.kafka.streams.kstream.Windowed;

import java.util.Objects;

public class SessionQueryBuilder implements QueryOperationBuilder {

    public static final String QUERY_PARAM_KEY = "key";
    public static final String QUERY_PARAM_KEY_FROM = "keyFrom";
    public static final String QUERY_PARAM_KEY_TO = "keyTo";

    private final String storeName;

    /**
     * Creates a new {@link SessionQueryBuilder} instance.
     * @param storeName     the storeName name.
     */
    SessionQueryBuilder(final String storeName) {
        Objects.requireNonNull(storeName, "storeName cannot be null");
        this.storeName = storeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query operation(final StoreOperation operation) {

        if (operation == StoreOperation.FETCH)
            return fetch();

        if (operation == StoreOperation.FETCH_KEY_RANGE)
            return fetchKeyRange();

        throw new InvalidQueryException("Operation not supported '" + operation.name() + "'");
    }

    public <K, V> Query<Windowed<K>, V> fetch() {
        return new Query<>(storeName, new FetchSessionQueryBuilder<>());
    }

    public <K, V> Query<Windowed<K>, V> fetchKeyRange() {
        return new Query<>(storeName, new SessionFetKeyRangeQueryBuilder<>());
    }
    static class FetchSessionQueryBuilder<K, V> implements LocalStoreQueryBuilder<Windowed<K>, V> {

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
        public LocalStoreQuery<Windowed<K>, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new SessionFetchQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY),
                null
            );
        }
    }

    static class SessionFetKeyRangeQueryBuilder<K, V> implements LocalStoreQueryBuilder<Windowed<K>, V> {

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
        public LocalStoreQuery<Windowed<K>, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new SessionFetchKeyRangeQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY_FROM),
                p.getValue(QUERY_PARAM_KEY_TO)
            );
        }
    }
}

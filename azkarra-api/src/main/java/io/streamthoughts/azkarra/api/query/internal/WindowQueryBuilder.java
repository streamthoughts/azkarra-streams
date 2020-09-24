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

import io.streamthoughts.azkarra.api.errors.Error;
import io.streamthoughts.azkarra.api.monad.Validator;
import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.QueryParams;
import org.apache.kafka.streams.kstream.Windowed;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Predicate;

public class WindowQueryBuilder implements QueryOperationBuilder {

    public static final String QUERY_PARAM_KEY = "key";
    public static final String QUERY_PARAM_KEY_FROM = "keyFrom";
    public static final String QUERY_PARAM_KEY_TO = "keyTo";
    public static final String QUERY_PARAM_TIME = "time";
    public static final String QUERY_PARAM_TIME_FROM = "timeFrom";
    public static final String QUERY_PARAM_TIME_TO = "timeTo";
    public static final Error INVALID_TIME_ERROR = new Error(
        "invalid parameters: 'timeFrom' must be inferior to 'timeTo'");

    protected final String storeName;

    /**
     * Creates a new {@link WindowQueryBuilder} instance.
     * @param storeName         the name of the store.
     */
    WindowQueryBuilder(final String storeName) {
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

        if (operation == StoreOperation.FETCH_TIME_RANGE)
            return fetchTimeRange();

        if (operation == StoreOperation.FETCH_ALL)
            return fetchAll();

        if (operation == StoreOperation.ALL)
            return all();

        throw new InvalidQueryException("Operation not supported '" + operation.name() + "'");
    }

    public <K, V> Query<K, V> fetch() {
        return new Query<>(storeName, new FetchWindowQueryBuilder<>());
    }

    public <K, V> Query<Windowed<K>, V> fetchKeyRange() {
        return new Query<>(storeName, new WindowFetchKeyRangeQueryBuilder<>());
    }

    public <V> Query<Long, V> fetchTimeRange() {
        return new Query<>(storeName, new WindowFetchTimeRangeQueryBuilder<>());
    }

    public <K, V> Query<Windowed<K>, V> fetchAll() {
        return new Query<>(storeName, new WindowFetchAllQueryBuilder<>());
    }

    public <K, V> Query<Windowed<K>, V> all() {
        return new Query<>(storeName, (store, parameters) -> new WindowGetAllQuery<>(store));
    }

    static class FetchWindowQueryBuilder<K, V> implements LocalStoreQueryBuilder<K, V> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validates(final QueryParams parameters) {
            return Validator.of(parameters)
                .validates(p -> p.contains(QUERY_PARAM_KEY), MissingRequiredKeyError.of(QUERY_PARAM_KEY))
                .validates(p -> p.contains(QUERY_PARAM_TIME), MissingRequiredKeyError.of(QUERY_PARAM_TIME));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<K, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new WindowFetchQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY),
                null,
                p.getLong(QUERY_PARAM_TIME)
            );
        }
    }

    static class WindowFetchKeyRangeQueryBuilder<K, V> implements LocalStoreQueryBuilder<Windowed<K>, V> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validates(final QueryParams parameters) {
            return Validator.of(parameters)
                .validates(p -> p.contains(QUERY_PARAM_KEY_FROM), MissingRequiredKeyError.of(QUERY_PARAM_KEY_FROM))
                .validates(p -> p.contains(QUERY_PARAM_KEY_TO), MissingRequiredKeyError.of(QUERY_PARAM_KEY_TO))
                .validates(p -> p.contains(QUERY_PARAM_TIME_FROM), MissingRequiredKeyError.of(QUERY_PARAM_TIME_FROM))
                .validates(p -> p.contains(QUERY_PARAM_TIME_TO), MissingRequiredKeyError.of(QUERY_PARAM_TIME_TO))
                .validates(new TimeValidator(), INVALID_TIME_ERROR);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<Windowed<K>, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new WindowFetchKeyRangeQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY_FROM),
                p.getValue(QUERY_PARAM_KEY_TO),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_TO))
            );
        }
    }

    static class WindowFetchTimeRangeQueryBuilder<V> implements LocalStoreQueryBuilder<Long, V> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validates(final QueryParams parameters) {
            return Validator.of(parameters)
                .validates(p -> p.contains(QUERY_PARAM_KEY), MissingRequiredKeyError.of(QUERY_PARAM_KEY))
                .validates(p -> p.contains(QUERY_PARAM_TIME_FROM), MissingRequiredKeyError.of(QUERY_PARAM_TIME_FROM))
                .validates(p -> p.contains(QUERY_PARAM_TIME_TO), MissingRequiredKeyError.of(QUERY_PARAM_TIME_TO))
                .validates(new TimeValidator(), INVALID_TIME_ERROR);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<Long, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new WindowFetchTimeRangeQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_TO))
            );
        }
    }

    static class WindowFetchAllQueryBuilder<K, V> implements LocalStoreQueryBuilder<Windowed<K>, V> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validates(final QueryParams parameters) {
            return Validator.of(parameters)
                .validates(p -> p.contains(QUERY_PARAM_TIME_FROM), MissingRequiredKeyError.of(QUERY_PARAM_TIME_FROM))
                .validates(p -> p.contains(QUERY_PARAM_TIME_TO), MissingRequiredKeyError.of(QUERY_PARAM_TIME_TO))
                .validates(new TimeValidator(), INVALID_TIME_ERROR);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalStoreQuery<Windowed<K>, V> build(final String store, final QueryParams parameters) {
            final QueryParams p = validates(parameters).getOrThrow(LocalStoreQueryBuilder::toInvalidQueryException);
            return new WindowFetchAllQuery<>(
                store,
                Instant.ofEpochMilli(p.getValue(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getValue(QUERY_PARAM_TIME_TO))
            );
        }
    }

    private static class TimeValidator implements Predicate<QueryParams> {

        @Override
        public boolean test(final QueryParams p) {
            return !p.contains(QUERY_PARAM_TIME_FROM) ||
                   !p.contains(QUERY_PARAM_TIME_TO) ||
                    p.getLong(QUERY_PARAM_TIME_TO) >= p.getLong(QUERY_PARAM_TIME_FROM);
        }
    }
}

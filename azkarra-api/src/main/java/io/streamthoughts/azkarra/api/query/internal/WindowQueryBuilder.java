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
import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.LocalPreparedQuery;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.error.InvalidQueryException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.kstream.Windowed;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Predicate;

import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_FROM;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_TO;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_TIME;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_TIME_FROM;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_TIME_TO;

public class WindowQueryBuilder implements QueryOperationBuilder {

    private static Serializer DEFAULT_SERIALIZER = new StringSerializer();

    public static final Error INVALID_TIME_ERROR = new Error(
        "invalid parameters: 'timeFrom' must be inferior to 'timeTo'");

    protected final String store;

    /**
     * Creates a new {@link WindowQueryBuilder} instance.
     *
     * @param store     the name of the store.
     */
    WindowQueryBuilder(final String store) {
        this.store = store;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalPreparedQuery<?, ?> prepare(final StoreOperation operation) {

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

    public <K, V> LocalPreparedQuery<K, V> fetch() {
        return new FetchWindowQueryBuilder<>(store);
    }

    public <K, V> LocalPreparedQuery<Windowed<K>, V> fetchKeyRange() {
        return new WindowFetchKeyRangeQueryBuilder<>(store);
    }

    public <K, V> LocalPreparedQuery<Long, V> fetchTimeRange() {
        return new WindowFetchTimeRangeQueryBuilder<>(store);
    }

    public <K, V> LocalPreparedQuery<Windowed<K>, V> fetchAll() {
        return new WindowFetchAllQueryBuilder<>(store);
    }

    public <K, V> LocalPreparedQuery<Windowed<K>, V> all() {
        return params -> new WindowGetAllQuery<>(store);
    }

    static class FetchWindowQueryBuilder<K, V> implements LocalPreparedQuery<K, V> {

        protected final String store;

        public FetchWindowQueryBuilder(final String store) {
            this.store = Objects.requireNonNull(store, "store should not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validator(final QueryParams params) {
            return Validator.of(params)
                .validates(p -> p.contains(QUERY_PARAM_KEY), MissingRequiredKeyError.of(QUERY_PARAM_KEY))
                .validates(p -> p.contains(QUERY_PARAM_TIME), MissingRequiredKeyError.of(QUERY_PARAM_TIME));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<K, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new WindowFetchQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY),
                DEFAULT_SERIALIZER,
                p.getLong(QUERY_PARAM_TIME)
            );
        }
    }

    static class WindowFetchKeyRangeQueryBuilder<K, V> implements LocalPreparedQuery<Windowed<K>, V> {

        protected final String store;

        public WindowFetchKeyRangeQueryBuilder(final String store) {
            this.store = Objects.requireNonNull(store, "store should not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validator(final QueryParams params) {
            return Validator.of(params)
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
        public LocalExecutableQuery<Windowed<K>, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new WindowFetchKeyRangeQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY_FROM),
                p.getValue(QUERY_PARAM_KEY_TO),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_TO))
            );
        }
    }

    static class WindowFetchTimeRangeQueryBuilder<K, V> implements LocalPreparedQuery<Long, V> {

        protected final String store;

        public WindowFetchTimeRangeQueryBuilder(final String store) {
            this.store = Objects.requireNonNull(store, "store should not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validator(final QueryParams params) {
            return Validator.of(params)
                .validates(p -> p.contains(QUERY_PARAM_KEY), MissingRequiredKeyError.of(QUERY_PARAM_KEY))
                .validates(p -> p.contains(QUERY_PARAM_TIME_FROM), MissingRequiredKeyError.of(QUERY_PARAM_TIME_FROM))
                .validates(p -> p.contains(QUERY_PARAM_TIME_TO), MissingRequiredKeyError.of(QUERY_PARAM_TIME_TO))
                .validates(new TimeValidator(), INVALID_TIME_ERROR);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<Long, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new WindowFetchTimeRangeQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_FROM)),
                Instant.ofEpochMilli(p.getLong(QUERY_PARAM_TIME_TO))
            );
        }
    }

    static class WindowFetchAllQueryBuilder<K, V> implements LocalPreparedQuery<Windowed<K>, V> {

        protected final String store;

        public WindowFetchAllQueryBuilder(final String store) {
            this.store = Objects.requireNonNull(store, "store should not be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Validator<QueryParams> validator(final QueryParams params) {
            return Validator.of(params)
                .validates(p -> p.contains(QUERY_PARAM_TIME_FROM), MissingRequiredKeyError.of(QUERY_PARAM_TIME_FROM))
                .validates(p -> p.contains(QUERY_PARAM_TIME_TO), MissingRequiredKeyError.of(QUERY_PARAM_TIME_TO))
                .validates(new TimeValidator(), INVALID_TIME_ERROR);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalExecutableQuery<Windowed<K>, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
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

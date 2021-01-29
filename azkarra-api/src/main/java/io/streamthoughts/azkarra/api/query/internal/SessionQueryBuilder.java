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
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.kstream.Windowed;

import java.util.Objects;

import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_FROM;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_TO;

public class SessionQueryBuilder implements QueryOperationBuilder {

    private static Serializer DEFAULT_SERIALIZER = new StringSerializer();

    private final String store;

    /**
     * Creates a new {@link SessionQueryBuilder} instance.
     *
     * @param store   the name of the store.
     */
    SessionQueryBuilder(final String store) {

        this.store = Objects.requireNonNull(store, "store should not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalPreparedQuery prepare(final StoreOperation operation) {

        if (operation == StoreOperation.FETCH)
            return fetch();

        if (operation == StoreOperation.FETCH_KEY_RANGE)
            return fetchKeyRange();

        throw new InvalidQueryException("Operation not supported '" + operation.name() + "'");
    }

    public <K, V> LocalPreparedQuery<Windowed<K>, V> fetch() {
        return new SessionFetchPreparedQuery<>();
    }

    public <K, V> LocalPreparedQuery<Windowed<K>, V> fetchKeyRange() {
        return new SessionFetKeyRangePreparedQuery<>();
    }

    class SessionFetchPreparedQuery<K, V> implements LocalPreparedQuery<Windowed<K>, V> {

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
        public LocalExecutableQuery<Windowed<K>, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new SessionFetchQuery<>(store, p.getValue(QUERY_PARAM_KEY), DEFAULT_SERIALIZER);
        }
    }

    class SessionFetKeyRangePreparedQuery<K, V> implements LocalPreparedQuery<Windowed<K>, V> {

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
        public LocalExecutableQuery<Windowed<K>, V> compile(final QueryParams params) {
            final QueryParams p = validator(params).getOrThrow(InvalidQueryException::new);
            return new SessionFetchKeyRangeQuery<>(
                store,
                p.getValue(QUERY_PARAM_KEY_FROM),
                p.getValue(QUERY_PARAM_KEY_TO)
            );
        }
    }
}

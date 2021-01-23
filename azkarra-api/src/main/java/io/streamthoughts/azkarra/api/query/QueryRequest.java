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
package io.streamthoughts.azkarra.api.query;

import io.streamthoughts.azkarra.api.errors.Error;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Class which is use to wrap information about the store to be query.
 */
public class QueryRequest implements Query {

    private String storeName;
    private StoreOperation storeOperation;
    private StoreType storeType;
    private QueryParams params = new GenericQueryParams();

    /**
     * Creates a new {@link QueryRequest} instance.
     */
    public QueryRequest() {
    }

    /**
     * Creates a new {@link QueryRequest} instance.
     */
    public QueryRequest(final Query query) {
        this(
            query.getStoreName(),
            query.getStoreType(),
            query.getStoreOperation(),
            query.getParams()
        );
    }

    /**
     * Creates a new {@link QueryRequest} instance.
     *
     * @param storeName         the name of the store to query.
     * @param storeType         the type of the store to query.
     * @param storeOperation    the operation to be executed on the store.
     * @param params        the params to be used for the operation.
     */
    public QueryRequest(final String storeName,
                        final StoreType storeType,
                        final StoreOperation storeOperation,
                        final QueryParams params) {
        this.storeName = storeName;
        this.storeOperation = storeOperation;
        this.storeType = storeType;
        this.params = params;
    }

    public QueryRequest storeName(final String storeName) {
        Objects.requireNonNull(storeName, "storeName should not be null");
        return new QueryRequest(
            storeName,
            storeType,
            storeOperation,
            params
        );
    }

    public QueryRequest storeType(final StoreType storeType) {
        Objects.requireNonNull(storeType, "storeType should not be null");
        return new QueryRequest(
            storeName,
            storeType,
            storeOperation,
            params
        );
    }

    public QueryRequest storeOperation(final StoreOperation storeOperation) {
        Objects.requireNonNull(storeOperation, "storeOperation should not be null");
        return new QueryRequest(
            storeName,
            storeType,
            storeOperation,
            params
        );
    }

    public QueryRequest params(final QueryParams params) {
        Objects.requireNonNull(params, "params should not be null");
        return new QueryRequest(
            storeName,
            storeType,
            storeOperation,
            params
        );
    }

    public <K, V> LocalPreparedQuery<K, V> prepare() {
        return storeType
            .getQueryBuilder(storeName)
            .prepare(storeOperation);
    }

    public <K, V> LocalExecutableQuery<K, V> compile() {
        final LocalPreparedQuery<K, V> prepared = prepare();
        return prepared.compile(params);
    }

    public Optional<List<Error>> validate() {
        return prepare().validate(params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryParams getParams() {
        return params;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStoreName() {
        return storeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreOperation getStoreOperation() {
        return storeOperation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreType getStoreType() {
        return storeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryRequest)) return false;
        QueryRequest that = (QueryRequest) o;
        return Objects.equals(storeName, that.storeName) &&
                storeOperation == that.storeOperation &&
                storeType == that.storeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(storeName, storeOperation, storeType);
    }

    @Override
    public String toString() {
        return "QueryRequest{" +
                "storeName=" + storeName +
                ", storeOperation=" + storeOperation +
                ", storeType=" + storeType +
                ", parameters=" + params +
                '}';
    }
}

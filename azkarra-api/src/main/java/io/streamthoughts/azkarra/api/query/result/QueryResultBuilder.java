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
package io.streamthoughts.azkarra.api.query.result;

import java.util.Collections;
import java.util.List;

public final class QueryResultBuilder<K, V> {

    /**
     * Creates a new builder instance.
     *
     * @return  a new {@link QueryResultBuilder} instance.
     */
    public static <K, V> QueryResultBuilder<K, V> newBuilder() {
        return new QueryResultBuilder<>();
    }

    private long took;

    private boolean timeout;

    private String server;

    private String storeName;

    private String storeType;

    private String error;

    private List<SuccessResultSet<K, V>> successResultSetList;

    private List<ErrorResultSet> failedResultSetList;

    private QueryStatus status;

    public QueryResultBuilder<K, V> setError(final String error) {
        this.error = error;
        return this;
    }

    public QueryResultBuilder<K, V> setTook(final long took) {
        this.took = took;
        return this;
    }

    public QueryResultBuilder<K, V> setTimeout(final boolean timeout) {
        this.timeout = timeout;
        return this;
    }

    public QueryResultBuilder<K, V>  setServer(final String server) {
        this.server = server;
        return this;
    }

    public QueryResultBuilder<K, V> setStatus(final QueryStatus status) {
        this.status = status;
        return this;
    }

    public QueryResultBuilder<K, V> setStoreName(final String storeName) {
        this.storeName = storeName;
        return this;
    }

    public QueryResultBuilder<K, V> setStoreType(final String storeType) {
        this.storeType = storeType;
        return this;
    }

    public QueryResultBuilder<K, V> setSuccessResultSet(final List<SuccessResultSet<K, V>> results) {
        this.successResultSetList = results;
        return this;
    }

    public QueryResultBuilder<K, V> setFailedResultSet(final ErrorResultSet result) {
       return setFailedResultSet(Collections.singletonList(result));
    }

    public QueryResultBuilder<K, V> setFailedResultSet(final List<ErrorResultSet> results) {
        this.failedResultSetList = results;
        return this;
    }

    public QueryResult<K, V> build() {
        return new QueryResult<>(
                took,
                timeout,
                server,
                status,
                new GlobalResultSet<>(
                    storeName,
                    storeType,
                    error,
                    failedResultSetList,
                    successResultSetList
                )
        );
    }
}

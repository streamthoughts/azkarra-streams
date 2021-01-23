/*
 * Copyright 2019-2021 StreamThoughts.
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
package io.streamthoughts.azkarra.runtime.query;

import io.streamthoughts.azkarra.api.query.DecorateQuery;
import io.streamthoughts.azkarra.api.query.Query;
import io.streamthoughts.azkarra.api.query.QueryCall;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.result.QueryResult;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class BaseAsyncQueryCall<K, V, Q extends Query> extends DecorateQuery<Q> implements QueryCall<K, V> {

    private CompletableFuture<QueryResult<K, V>> future;

    private volatile boolean isExecuted = false;

    /**
     * Creates a new {@link BaseAsyncQueryCall} instance.
     *
     * @param query the query.
     */
    protected BaseAsyncQueryCall(Q query) {
        super(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<QueryResult<K, V>> executeAsync(final QueryOptions options) {
        return executeUsingForkJoin(options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeAsync(final QueryOptions options, final Consumer<QueryResult<K, V>> resultCallback) {
        executeUsingForkJoin(options).thenAcceptAsync(resultCallback);
    }

    private CompletableFuture<QueryResult<K, V>> executeUsingForkJoin(final QueryOptions options) {
        future = CompletableFuture.supplyAsync(() -> {
            Objects.requireNonNull(options, "options should not be null");
            return execute(options);
        });
        setIsExecuted(true);
        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
        if (future != null) {
            future.cancel(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExecuted() {
        return isExecuted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCanceled() {
        return future != null && future.isCancelled();
    }

    public void setIsExecuted(final boolean isExecuted) {
        this.isExecuted = isExecuted;
    }

}

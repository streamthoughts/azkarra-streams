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
import io.streamthoughts.azkarra.api.query.QueryCall;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.util.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static io.streamthoughts.azkarra.runtime.query.internal.QueryResultUtils.buildInternalErrorResult;

public class RemoteQueryCall<K, V> extends BaseAsyncQueryCall<K, V, QueryRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteQueryCall.class);

    private final String application;
    private final Endpoint endpoint;
    private final RemoteStateStoreClient client;

    private final String localServerName;
    /**
     * Creates a new {@link DecorateQuery} instance.
     *
     * @param query the query.
     */
    public RemoteQueryCall(final String application,
                           final String localServerName,
                           final Endpoint endpoint,
                           final QueryRequest query,
                           final RemoteStateStoreClient client) {
        super(query);
        this.application = application;
        this.endpoint = endpoint;
        this.client = client;
        this.localServerName = localServerName;
    }

    @Override
    public QueryResult<K, V> execute(final QueryOptions options) {
        QueryResult<K, V> result;
        try {
            result = executeAsync(options).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = buildInternalErrorResult(localServerName, endpoint.toString(), e);
        } catch (ExecutionException e) {
            // cast should be OK.
            LOG.error("Cannot query remote state store. {}", e.getCause().getMessage());
            throw (RuntimeException)e.getCause();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<QueryResult<K, V>> executeAsync(final QueryOptions options) {
        CompletableFuture<QueryResult<K, V>> future = client.query(application, endpoint, query, options);
        if (options.retries() == 0) {
            future = future.exceptionally(t -> buildInternalErrorResult(localServerName, endpoint.toString(), t));
        }
        return future.thenApply(rs -> rs.server(localServerName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeAsync(final QueryOptions options, final Consumer<QueryResult<K, V>> resultCallback) {
        executeAsync(options).thenAccept(resultCallback);
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isExecuted() {
        return false;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public QueryCall<K, V> renew() {
        return null;
    }
}

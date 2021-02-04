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
package io.streamthoughts.azkarra.http.query;

import io.streamthoughts.azkarra.api.errors.AzkarraRetriableException;
import io.streamthoughts.azkarra.api.query.DecorateQuery;
import io.streamthoughts.azkarra.api.query.QueryCall;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.util.Endpoint;
import io.streamthoughts.azkarra.client.openapi.ApiException;
import io.streamthoughts.azkarra.client.openapi.apis.AzkarraV1Api;
import io.streamthoughts.azkarra.client.openapi.models.V1Query;
import okhttp3.Call;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static io.streamthoughts.azkarra.runtime.query.internal.QueryResultUtils.buildInternalErrorResult;

/**
 * A {@code RestApiQueryCall} can be used to execute an interactive query on a remote instance.
 *
 * @param <K>   the expected type for the record key.
 * @param <V>   the expected type for the record value.
 */
public class RestApiQueryCall<K, V> extends DecorateQuery<QueryRequest> implements QueryCall<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(RestApiQueryCall.class);

    private final String applicationId;
    private final Endpoint localEndpoint;
    private final Endpoint remoteEndpoint;
    private final AzkarraV1Api api;

    private Call call;

    /**
     * Creates a new {@link RestApiQueryCall} instance.
     *
     * @param applicationId  the {@code application.id}.
     * @param localEndpoint  the local {@link Endpoint} from which this remote query is executed.
     * @param remoteEndpoint the remote {@link Endpoint} to query.
     * @param api            the {@link AzkarraV1Api}.
     * @param request        the {@link QueryRequest} to execute.
     */
    public RestApiQueryCall(final String applicationId,
                            final Endpoint localEndpoint,
                            final Endpoint remoteEndpoint,
                            final AzkarraV1Api api,
                            final QueryRequest request) {
        super(request);
        this.api = api;
        this.applicationId = applicationId;
        this.localEndpoint = localEndpoint;
        this.remoteEndpoint = remoteEndpoint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResult<K, V> execute(final QueryOptions options) {
        QueryResult<K, V> result;
        try {
            result = executeAsync(options).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = buildInternalErrorResult(localEndpoint.listener(), remoteEndpoint.listener(), e);
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
        if (call != null) throw new IllegalStateException("Cannot executed this QueryCall, query already executed");

        final V1Query v1Query = V1QueryBuilder.create(query, options);
        final CompletableFuture<QueryResult<K, V>> future = new CompletableFuture<>();
        try {
            call = api.queryStateStoreAsync(
                applicationId,
                getStoreName(),
                v1Query,
                getCallback(future)
            );
            return future;
        } catch (ApiException e) {
            future.completeExceptionally(new AzkarraRetriableException(e));
        }
        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeAsync(final QueryOptions options,
                             final Consumer<QueryResult<K, V>> resultCallback) {
        executeAsync(options).thenAcceptAsync(resultCallback);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
        if (call != null) {
            call.cancel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExecuted() {
        return call != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCanceled() {
        return call != null && call.isCanceled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryCall<K, V> renew() {
        return new RestApiQueryCall<>(applicationId, localEndpoint, remoteEndpoint, api, query);
    }

    @NotNull
    private AsyncQueryApiCallback<K, V> getCallback(final CompletableFuture<QueryResult<K, V>> future) {
        return new AsyncQueryApiCallback<>(future, (e, statusCode, responseHeaders) -> {
            // The client has failed to send the request, failed the future with
            // the ApiException so that the query can be retried by the caller.
            if (statusCode == 0) {
                future.completeExceptionally(new AzkarraRetriableException(e));
            } else {
                //
                var error = new QueryError("Invalid response from remote server (statusCode:'" + statusCode + "')");
                future.complete(QueryResultBuilder.<K, V>newBuilder()
                    .setStatus(QueryStatus.ERROR)
                    .setServer(localEndpoint.listener())
                    .setStoreName(query.getStoreName())
                    .setStoreType(query.getStoreType().prettyName())
                    .setFailedResultSet(new ErrorResultSet(remoteEndpoint.listener(), true, error))
                    .setTook(0L)
                    .build()
                );
            }
        });
    }
}

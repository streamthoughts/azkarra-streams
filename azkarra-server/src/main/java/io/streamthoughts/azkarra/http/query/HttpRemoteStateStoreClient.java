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
package io.streamthoughts.azkarra.http.query;

import io.streamthoughts.azkarra.api.errors.AzkarraRetriableException;
import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.internal.QueryConstants;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;
import io.streamthoughts.azkarra.api.util.Endpoint;
import io.streamthoughts.azkarra.client.openapi.ApiCallback;
import io.streamthoughts.azkarra.client.openapi.ApiClient;
import io.streamthoughts.azkarra.client.openapi.ApiException;
import io.streamthoughts.azkarra.client.openapi.apis.AzkarraV1Api;
import io.streamthoughts.azkarra.client.openapi.models.V1Error;
import io.streamthoughts.azkarra.client.openapi.models.V1Query;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryErrorResultSet;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryFetchAllParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryFetchKeyRangeParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryFetchParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryFetchTimeRangeParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryGetParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryOperation;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryOptions;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryRangeParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryResult;
import io.streamthoughts.azkarra.client.openapi.models.V1QuerySuccessResultSet;
import io.streamthoughts.azkarra.client.openapi.models.V1Record;
import io.streamthoughts.azkarra.runtime.query.RemoteStateStoreClient;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class HttpRemoteStateStoreClient implements RemoteStateStoreClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRemoteStateStoreClient.class);

    private static final String DEFAULT_SERVER_URL_PROTOCOL = "http";

    private final OkHttpClient httpClient;

    private final String protocol;

    /**
     * Creates a new {@link HttpRemoteStateStoreClient} instance.
     *
     * @param httpClient     the {@link OkHttpClient} instance.
     */
    public HttpRemoteStateStoreClient(final OkHttpClient httpClient) {
        this(httpClient, DEFAULT_SERVER_URL_PROTOCOL);
    }

    /**
     * Creates a new {@link HttpRemoteStateStoreClient} instance.
     *
     * @param httpClient     the {@link OkHttpClient} instance.
     */
    public HttpRemoteStateStoreClient(final OkHttpClient httpClient, final String protocol) {
        this.httpClient =  Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.protocol = Objects.requireNonNull(protocol, "protocol cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> CompletableFuture<QueryResult<K, V>> query(final String application,
                                                             final Endpoint endpoint,
                                                             final QueryRequest queryObject,
                                                             final QueryOptions queryOptions) {

        final CompletableFuture<QueryResult<K, V>> future = new CompletableFuture<>();
        try {
            final String server = endpoint.listener();
            final QueryResultBuilder<K, V> builder = QueryResultBuilder.<K, V>newBuilder()
                .setServer(server)
                .setStoreName(queryObject.getStoreName())
                .setStoreType(queryObject.getStoreType().prettyName());

            final V1Query v1query = createNewQuery(queryObject, queryOptions);

            final String url = protocol + "://" + endpoint.address()+ ":" + endpoint.port();
            LOG.debug("Forwarding state store queryObject to remote server {}", url);
            final ApiClient apiClient = new ApiClient()
                    .setBasePath(url)
                    .setHttpClient(httpClient);

            new AzkarraV1Api(apiClient)
                .queryStateStoreAsync(
                    application,
                    queryObject.getStoreName(),
                    v1query, new AsyncQueryCallback<>(server, future, builder)
                );
        } catch (ApiException e) {
            future.completeExceptionally(new AzkarraRetriableException(e));
        }
        return future;
    }

    private V1Query createNewQuery(QueryRequest query, QueryOptions options) {
        final QueryParams params = query.getParams();
        final V1QueryOperation operation = new V1QueryOperation();
        switch (query.getStoreOperation()) {
            case GET:
                operation.get(new V1QueryGetParams()
                    .key(params.getString(QueryConstants.QUERY_PARAM_KEY))
                );
                break;
            case ALL:
                operation.all(Collections.emptyMap());
                break;
            case FETCH:
                operation.fetch(new V1QueryFetchParams()
                    .key(params.getString(QueryConstants.QUERY_PARAM_KEY))
                    .time(params.getLong(QueryConstants.QUERY_PARAM_TIME))
                );
                break;
            case FETCH_KEY_RANGE:
                operation.fetchKeyRange(new V1QueryFetchKeyRangeParams()
                    .keyFrom(params.getString(QueryConstants.QUERY_PARAM_KEY_FROM))
                    .keyTo(params.getString(QueryConstants.QUERY_PARAM_KEY_TO))
                    .timeFrom(params.getLong(QueryConstants.QUERY_PARAM_TIME_FROM))
                    .timeTo(params.getLong(QueryConstants.QUERY_PARAM_TIME_TO))
                );
                break;
            case FETCH_TIME_RANGE:
                operation.fetchTimeRange(new V1QueryFetchTimeRangeParams()
                    .key(params.getString(QueryConstants.QUERY_PARAM_KEY))
                    .timeFrom(params.getLong(QueryConstants.QUERY_PARAM_TIME_FROM))
                    .timeTo(params.getLong(QueryConstants.QUERY_PARAM_TIME_TO))
                );
                break;
            case FETCH_ALL:
                operation.fetchAll(new V1QueryFetchAllParams()
                    .timeFrom(params.getLong(QueryConstants.QUERY_PARAM_TIME_FROM))
                    .timeTo(params.getLong(QueryConstants.QUERY_PARAM_TIME_TO))
                );
                break;
            case RANGE:
                operation.range(new V1QueryRangeParams()
                    .keyFrom(params.getString(QueryConstants.QUERY_PARAM_KEY_FROM))
                    .keyTo(params.getString(QueryConstants.QUERY_PARAM_KEY_TO))
                );
                break;
            case COUNT:
                operation.count(Collections.emptyMap());
                break;
        }

        return new V1Query()
            .type(V1Query.TypeEnum.fromValue(query.getStoreType().prettyName()))
            .query(operation)
            .setOptions(
                new V1QueryOptions()
                    .limit(options.limit())
                    .queryTimeoutMs(options.queryTimeout().toMillis())
                    .remoteAccessAllowed(options.remoteAccessAllowed())
                    .retryBackoffMs(options.retryBackoff().toMillis())
                    .retries(options.retries())
            );
    }

    private static class AsyncQueryCallback<K, V> implements ApiCallback<V1QueryResult> {

        private final String remoteServerName;
        private final QueryResultBuilder<K, V> builder;
        private final CompletableFuture<QueryResult<K, V>> completableFuture;

        /**
         * Creates a new {@link AsyncQueryCallback} instance.
         *
         * @param completableFuture the {@link CompletableFuture} instance.
         * @param builder           the {@link QueryResultBuilder} to be used.
         */
        AsyncQueryCallback(final String remoteServerName,
                           final CompletableFuture<QueryResult<K, V>> completableFuture,
                           final QueryResultBuilder<K, V> builder) {
            this.remoteServerName = remoteServerName;
            this.completableFuture = completableFuture;
            this.builder = builder;

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onFailure(final ApiException e, final int statusCode, final Map responseHeaders) {
            // The client has failed to send the request, failed the future with
            // the ApiException so that the query can be retried by the caller.
            if (statusCode == 0) {
                completableFuture.completeExceptionally(new AzkarraRetriableException(e));
            } else {
                //
                final QueryError error = new QueryError("Invalid response from remote server (statusCode:'" + statusCode + "')");
                final QueryResult<K, V> result = buildQueryResultFor(remoteServerName, error);
                completableFuture.complete(result);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public void onSuccess(V1QueryResult result, int statusCode, Map<String, List<String>> responseHeaders) {
            final QueryResultBuilder<Object, Object> builder = QueryResultBuilder.newBuilder()
                    .setServer(result.getServer())
                    .setTook(result.getTook())
                    .setStatus(QueryStatus.valueOf(result.getStatus().getValue()))
                    .setStoreName(result.getResult().getStore())
                    .setStoreType(result.getResult().getType())
                    .setError(result.getResult().getError());

            final List<V1QuerySuccessResultSet> v1QuerySuccessResultSetList = result.getResult().getSuccess();
            if (v1QuerySuccessResultSetList != null) {
                final List<SuccessResultSet<Object, Object>> successResultSets = new ArrayList<>(v1QuerySuccessResultSetList.size());
                for (V1QuerySuccessResultSet it : v1QuerySuccessResultSetList) {
                    final List<V1Record> v1Records = it.getRecords();
                    if (v1Records != null) {
                        final List<KV<Object, Object>> records = new ArrayList<>(v1Records.size());
                        for (V1Record v1Record : v1Records) {
                            records.add(new KV<>(v1Record.getKey(), v1Record.getValue()));
                        }
                        successResultSets.add(new SuccessResultSet<>(it.getServer(), it.getRemote(), records));
                    }
                }
                builder.setSuccessResultSet(successResultSets);
            }
            final List<V1QueryErrorResultSet> v1QueryErrorResultSetList = result.getResult().getFailure();
            if (v1QueryErrorResultSetList != null) {
                final List<ErrorResultSet> errorResultSets = new ArrayList<>(v1QueryErrorResultSetList.size());
                for (V1QueryErrorResultSet it : v1QueryErrorResultSetList) {
                    final List<V1Error> v1Errors = it.getErrors();
                    if (v1Errors != null) {
                        final List<QueryError> errors = new ArrayList<>(v1Errors.size());
                        for (V1Error error : v1Errors) {
                            errors.add(new QueryError(error.getMessage()));
                        }
                        errorResultSets.add(new ErrorResultSet(it.getServer(), it.getRemote(), errors));
                    }
                }
                builder.setFailedResultSet(errorResultSets);
            }
            completableFuture.complete((QueryResult<K, V>)builder.build());
        }

        @Override
        public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {

        }

        @Override
        public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {

        }

        private QueryResult<K, V> buildQueryResultFor(final String remoteServerName, final QueryError e) {
            final ErrorResultSet result = new ErrorResultSet(remoteServerName, true, e);
            return builder.setStatus(QueryStatus.ERROR)
                    .setFailedResultSet(result)
                    .setTook(0L)
                    .build();
        }
    }
}

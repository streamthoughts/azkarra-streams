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

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;
import io.streamthoughts.azkarra.client.openapi.ApiCallback;
import io.streamthoughts.azkarra.client.openapi.ApiException;
import io.streamthoughts.azkarra.client.openapi.models.V1Error;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryErrorResultSet;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryResult;
import io.streamthoughts.azkarra.client.openapi.models.V1QuerySuccessResultSet;
import io.streamthoughts.azkarra.client.openapi.models.V1Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AsyncQueryApiCallback<K, V> implements ApiCallback<V1QueryResult> {

    private final ApiOnFailureCallback onFailureCallback;
    private final CompletableFuture<QueryResult<K, V>> completableFuture;

    /**
     * Creates a new {@link AsyncQueryApiCallback} instance.
     *
     * @param completableFuture the {@link CompletableFuture} instance.
     */
    public AsyncQueryApiCallback(final CompletableFuture<QueryResult<K, V>> completableFuture,
                                 final ApiOnFailureCallback onFailureCallback) {
        this.completableFuture = completableFuture;
        this.onFailureCallback = onFailureCallback;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFailure(final ApiException e, final int statusCode, final Map responseHeaders) {
        onFailureCallback.onFailure(e, statusCode, responseHeaders);
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
            final List<SuccessResultSet<Object, Object>> successResultSets =
                    new ArrayList<>(v1QuerySuccessResultSetList.size());
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
        completableFuture.complete((QueryResult<K, V>) builder.build());
    }

    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {

    }

    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {

    }
}
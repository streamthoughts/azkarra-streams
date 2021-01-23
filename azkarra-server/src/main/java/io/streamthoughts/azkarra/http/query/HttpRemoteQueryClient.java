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
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.streams.ServerHostInfo;
import io.streamthoughts.azkarra.runtime.query.RemoteStateStoreClient;
import io.streamthoughts.azkarra.serialization.Serdes;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class HttpRemoteQueryClient implements RemoteStateStoreClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRemoteQueryClient.class);

    private final OkHttpClient client;

    private final QueryURLBuilder queryURLBuilder;

    private final Serdes<QueryResult> serdes;

    private final MediaType mediaType;

    /**
     * Creates a new {@link HttpRemoteQueryClient} instance.
     *
     * @param httpClient        the {@link OkHttpClient} instance.
     * @param queryURLBuilder   the {@link QueryURLBuilder} instance.
     */
    public HttpRemoteQueryClient(final OkHttpClient httpClient,
                                 final QueryURLBuilder queryURLBuilder,
                                 final Serdes<QueryResult> serdes) {
        this.client =  Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.queryURLBuilder = Objects.requireNonNull(queryURLBuilder, "queryURLBuilder cannot be null");
        this.serdes = Objects.requireNonNull(serdes, "serdes cannot be null");
        this.mediaType = MediaType.get(serdes.contentType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> CompletableFuture<QueryResult<K, V>> query(final ServerHostInfo serverInfo,
                                                             final QueryRequest query,
                                                             final QueryOptions options) {
        final String server = serverInfo.hostAndPort();

        final String path = queryURLBuilder.buildURL(server, serverInfo.id(), query.getStoreName());

        final String json = JsonQuerySerde.serialize(query, options);

        Request request = new Request.Builder()
                .url(path)
                .addHeader("Accept", mediaType.toString())
                .addHeader("Content-type", mediaType.toString())
                .post(RequestBody.create(json, mediaType))
                .build();

        final QueryResultBuilder<K, V> builder = QueryResultBuilder.<K, V>newBuilder()
                .setServer(server)
                .setStoreName(query.getStoreName())
                .setStoreType(query.getStoreType().prettyName());

        final CompletableFuture<QueryResult<K, V>> future = new CompletableFuture<>();

        LOG.debug("Forwarding state store query to remote server {}", server);
        client.newCall(request).enqueue(new AsyncQueryCallback<>(server, future, builder, serdes));
        return future;
    }

    private static class AsyncQueryCallback<K, V> implements Callback {

        private final String remoteServerName;
        private final QueryResultBuilder<K, V> builder;
        private final CompletableFuture<QueryResult<K, V>> completableFuture;
        private final Serdes<QueryResult> serdes;

        /**
         * Creates a new {@link AsyncQueryCallback} instance.
         *
         * @param completableFuture the {@link CompletableFuture} instance.
         * @param builder           the {@link QueryResultBuilder} to be used.
         */
        AsyncQueryCallback(final String remoteServerName,
                           final CompletableFuture<QueryResult<K, V>> completableFuture,
                           final QueryResultBuilder<K, V> builder,
                           final Serdes<QueryResult> serdes) {
            this.remoteServerName = remoteServerName;
            this.completableFuture = completableFuture;
            this.builder = builder;
            this.serdes = serdes;

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onFailure(final Call call, final IOException e) {
            // Failed the future with the IOException so that que the query can be retried by the caller.
            completableFuture.completeExceptionally(new AzkarraRetriableException(e));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public void onResponse(final Call call, final Response response) {
            try (ResponseBody responseBody = response.body()) {
                try {
                    final byte[] payload = responseBody.bytes();
                    int code = response.code();
                    if (code >= 200 && code < 300) {
                        completableFuture.complete(serdes.deserialize(payload));
                    } else {
                        final QueryError error = new QueryError(
                            "Invalid response from remote server (code:'" + code + "') : "
                            + Arrays.toString(payload));
                        final QueryResult<K, V> result = buildQueryResultFor(remoteServerName, error);
                        completableFuture.complete(result);
                    }
                } catch (final Exception e) {
                    completableFuture.complete(buildQueryResultFor(remoteServerName, QueryError.of(e)));
                }
            }
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

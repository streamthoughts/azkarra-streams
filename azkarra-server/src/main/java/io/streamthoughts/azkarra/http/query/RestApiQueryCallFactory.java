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

import io.streamthoughts.azkarra.api.query.QueryCall;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.util.Endpoint;
import io.streamthoughts.azkarra.client.openapi.ApiClient;
import io.streamthoughts.azkarra.client.openapi.apis.AzkarraV1Api;
import io.streamthoughts.azkarra.runtime.query.RemoteQueryCallFactory;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RestApiQueryCallFactory implements RemoteQueryCallFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RestApiQueryCallFactory.class);

    private static final String DEFAULT_SERVER_URL_PROTOCOL = "http";

    private final OkHttpClient httpClient;

    private final String protocol;

    /**
     * Creates a new {@link RestApiQueryCallFactory} instance.
     *
     * @param httpClient     the {@link OkHttpClient} instance.
     */
    public RestApiQueryCallFactory(final OkHttpClient httpClient) {
        this(httpClient, DEFAULT_SERVER_URL_PROTOCOL);
    }

    /**
     * Creates a new {@link RestApiQueryCallFactory} instance.
     *
     * @param httpClient     the {@link OkHttpClient} instance.
     */
    public RestApiQueryCallFactory(final OkHttpClient httpClient, final String protocol) {
        this.httpClient =  Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.protocol = Objects.requireNonNull(protocol, "protocol cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> QueryCall<K, V> create(final String applicationId,
                                         final Endpoint localEndpoint,
                                         final Endpoint remoteEndpoint,
                                         final QueryRequest queryRequest) {
        final String url = protocol + "://" + remoteEndpoint.address()+ ":" + remoteEndpoint.port();
        LOG.debug("Forwarding state store queryRequest to remote server {}", url);
        final ApiClient apiClient = new ApiClient()
                .setBasePath(url)
                .setHttpClient(httpClient);
        final AzkarraV1Api api = new AzkarraV1Api(apiClient);
        return new RestApiQueryCall<K, V>(applicationId, localEndpoint, remoteEndpoint, api, queryRequest);
    }
}

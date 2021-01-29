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

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.query.GenericQueryParams;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.StoreType;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;
import io.streamthoughts.azkarra.api.util.Endpoint;
import io.streamthoughts.azkarra.http.APIVersions;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.client.HttpClientBuilder;
import io.streamthoughts.azkarra.http.serialization.json.SpecificJsonSerdes;
import io.streamthoughts.azkarra.serialization.Serdes;
import io.streamthoughts.azkarra.serialization.json.Json;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonList;

public class HttpRemoteQueryClientTest {

    private static final Serdes<QueryResult> SERDES = new SpecificJsonSerdes<>(
        Json.getDefault(),
        QueryResult.class
    );

    private static final Endpoint SERVER_INFO = new Endpoint("localhost", 8089);

    private static final String TEST_STORE_NAME = "store";

    private HttpRemoteQueryClient client;

    private MockWebServer server;

    @BeforeEach
    public void beforeEach() throws IOException {
        client = new HttpRemoteQueryClient(
            HttpClientBuilder.newBuilder().build(),
            new QueryURLBuilder.DefaultQueryURLBuilder("http", APIVersions.PATH_V1),
            new SpecificJsonSerdes<>(ExchangeHelper.JSON, QueryResult.class)
        );
        server = new MockWebServer();
        server.start(SERVER_INFO.port());
    }

    @AfterEach
    public void afterEach() throws IOException {
        server.shutdown();
    }

    @Test
    public void shouldQueryRemoteServerSuccessfully() throws ExecutionException, InterruptedException {
        QueryRequest query = new QueryRequest(
            TEST_STORE_NAME,
            StoreType.KEY_VALUE,
            StoreOperation.ALL,
            GenericQueryParams.empty()
        );

        QueryResult<String, String> queryResult = newQueryResult();

        server.enqueue(new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(new String(SERDES.serialize(queryResult)))
        );

        CompletableFuture<QueryResult<Object, Object>> future = client.query("test", SERVER_INFO, query, QueryOptions.immediately());
        QueryResult<Object, Object> response = future.get();
        Assertions.assertEquals(queryResult, response);
    }

    private QueryResult<String, String> newQueryResult() {
        List<KV<String, String>> kv = singletonList(KV.of("k1", "v1"));
        return QueryResultBuilder.<String, String>newBuilder()
            .setServer(SERVER_INFO.listener())
            .setStatus(QueryStatus.SUCCESS)
            .setStoreName(TEST_STORE_NAME)
            .setSuccessResultSet(
                singletonList(
                    new SuccessResultSet<>(SERVER_INFO.listener(), true, kv)
                )
            )
            .setTook(0)
            .build();
    }

}
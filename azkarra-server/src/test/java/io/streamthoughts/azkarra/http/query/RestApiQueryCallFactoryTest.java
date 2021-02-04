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
import io.streamthoughts.azkarra.client.HttpClientBuilder;
import io.streamthoughts.azkarra.http.serialization.json.SpecificJsonSerdes;
import io.streamthoughts.azkarra.serialization.Serdes;
import io.streamthoughts.azkarra.serialization.json.Json;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonList;

public class RestApiQueryCallFactoryTest {

    private static final Serdes<QueryResult> SERDES = new SpecificJsonSerdes<>(
        Json.getDefault(),
        QueryResult.class
    );

    private static final String TEST_STORE_NAME = "store";

    private final RestApiQueryCallFactory callFactory = new RestApiQueryCallFactory(
            HttpClientBuilder.newBuilder().build()
    );

    private static MockWebServer SERVER;

    private static Endpoint ENDPOINT;


    @BeforeAll
    public static void beforeAll() throws IOException {
        SERVER = new MockWebServer();
        SERVER.start();
        ENDPOINT = new Endpoint("localhost", SERVER.getPort());
    }

    @AfterAll
    public static void afterAll() throws IOException {
        SERVER.shutdown();
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

        SERVER.enqueue(new MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(new String(SERDES.serialize(queryResult)))
        );

        CompletableFuture<QueryResult<Object, Object>> future = callFactory
            .create("test", Endpoint.of("localhost:8080"), ENDPOINT, query)
            .executeAsync(QueryOptions.immediately());
        QueryResult<Object, Object> response = future.get();
        Assertions.assertEquals(queryResult, response);
    }

    private QueryResult<String, String> newQueryResult() {
        List<KV<String, String>> kv = singletonList(KV.of("k1", "v1"));
        return QueryResultBuilder.<String, String>newBuilder()
            .setServer(ENDPOINT.listener())
            .setStatus(QueryStatus.SUCCESS)
            .setStoreName(TEST_STORE_NAME)
            .setSuccessResultSet(
                singletonList(
                    new SuccessResultSet<>(ENDPOINT.listener(), true, kv)
                )
            )
            .setTook(0)
            .build();
    }

}
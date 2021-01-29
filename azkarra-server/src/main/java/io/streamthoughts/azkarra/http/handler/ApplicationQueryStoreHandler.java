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
package io.streamthoughts.azkarra.http.handler;

import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.query.InteractiveQueryService;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.serialization.json.JsonQuerySerde;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.http.utils.Constants.HTTP_QUERY_PARAM_ID;
import static io.streamthoughts.azkarra.http.utils.Constants.HTTP_QUERY_PARAM_STORE;

public class ApplicationQueryStoreHandler implements HttpHandler {

    private final InteractiveQueryService service;

    private final boolean onlySuccessKVRecords;

    /**
     * Creates a new {@link ApplicationQueryStoreHandler} instance.
     *
     * @param service   the {@link AzkarraStreamsService} instance.
     */
    public ApplicationQueryStoreHandler(final InteractiveQueryService service,
                                        final boolean onlySuccessKVRecords) {
        this.service = service;
        this.onlySuccessKVRecords = onlySuccessKVRecords;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws IOException {
        final String applicationId = ExchangeHelper.getQueryParam(exchange, HTTP_QUERY_PARAM_ID);
        final String store = ExchangeHelper.getQueryParam(exchange, HTTP_QUERY_PARAM_STORE);

        byte[] data = exchange.getInputStream().readAllBytes();
        Tuple<QueryRequest, QueryOptions> query = JsonQuerySerde.deserialize(store, data);

        final QueryResult<Object, Object> result = service.execute(applicationId, query.left(), query.right());
        ExchangeHelper.sendJsonResponse(exchange, onlySuccessKVRecords ? onlySuccessKVRecords(result): result);
    }

    private static List<KV<Object, Object>> onlySuccessKVRecords(final QueryResult<Object, Object> result) {
        return result
            .getResult()
            .getSuccess()
            .stream()
            .flatMap(s -> s.getRecords().stream())
            .collect(Collectors.toList());
    }
}


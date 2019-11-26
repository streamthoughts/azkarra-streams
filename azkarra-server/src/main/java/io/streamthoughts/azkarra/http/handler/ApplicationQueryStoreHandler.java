/*
 * Copyright 2019 StreamThoughts.
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

import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.query.Queried;
import io.streamthoughts.azkarra.api.query.QueryInfo;
import io.streamthoughts.azkarra.api.query.internal.Query;
import io.streamthoughts.azkarra.http.query.JsonQuerySerde;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;

public class ApplicationQueryStoreHandler extends AbstractStreamHttpHandler implements WithApplication {

    private static final String QUERY_PARAM_STORE_NAME = "storeName";

    /**
     * Creates a new {@link ApplicationQueryStoreHandler} instance.
     *
     * @param service   the {@link AzkarraStreamsService} instance.
     */
    public ApplicationQueryStoreHandler(final AzkarraStreamsService service) {
        super(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange, final String applicationId) throws IOException {
        final String store = ExchangeHelper.getQueryParam(exchange, QUERY_PARAM_STORE_NAME);

        byte[] data = exchange.getInputStream().readAllBytes();
        Tuple<QueryInfo, Queried> deserialized = JsonQuerySerde.deserialize(store, data);

        QueryInfo queryInfo = deserialized.left();

        Query<Object, Object> query = queryInfo.type().buildQuery(queryInfo.storeName(), queryInfo.operation());
        final QueryResult<Object, Object> result = service.query(
                applicationId,
                query,
                queryInfo.parameters(),
                deserialized.right()
        );
        ExchangeHelper.sendJsonResponse(exchange, result);
    }
}


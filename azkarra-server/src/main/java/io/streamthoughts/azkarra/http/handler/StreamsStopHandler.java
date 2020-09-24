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

import com.fasterxml.jackson.databind.JsonNode;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.undertow.server.HttpServerExchange;

public class StreamsStopHandler extends AbstractStreamHttpHandler implements WithApplication {

    private static final String CLEANUP_QUERY_PARAM = "cleanup";

    /**
     * Creates a new {@link StreamsStopHandler} instance.
     *
     * @param service   the {@link AzkarraStreamsService} instance.
     */
    public StreamsStopHandler(final AzkarraStreamsService service) {
        super(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange, final String applicationId) {
        final JsonNode payload = ExchangeHelper.readJsonRequest(exchange);
        boolean cleanup = false;
        if (!payload.isMissingNode() && payload.hasNonNull(CLEANUP_QUERY_PARAM)) {
            JsonNode node = payload.get(CLEANUP_QUERY_PARAM);
            cleanup = node.asBoolean();
        }
        service.stopStreams(applicationId, cleanup);
    }
}

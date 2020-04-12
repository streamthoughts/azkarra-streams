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

import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.streams.consumer.ConsumerGroupOffsets;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.undertow.server.HttpServerExchange;

public class StreamsGetOffsetsHandler extends AbstractStreamHttpHandler implements WithApplication {

    /**
     * Creates a new {@link StreamsGetOffsetsHandler} instance.
     *
     * @param service   the {@link AzkarraStreamsService} instance.
     */
    public StreamsGetOffsetsHandler(final AzkarraStreamsService service) {
        super(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange, final String applicationId) {
        final ConsumerGroupOffsets offsets = service.getStreamsConsumerOffsetsById(applicationId);
        ExchangeHelper.sendJsonResponse(exchange, offsets);
    }
}
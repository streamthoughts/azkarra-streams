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
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.undertow.server.HttpServerExchange;

public class ContextGetHandler extends AbstractStreamHttpHandler  {

    /**
     * Creates a new {@link ContextGetHandler} instance.
     *
     * @param service the {@link AzkarraStreamsService} instance.
     */
    public ContextGetHandler(final AzkarraStreamsService service) {
        super(service);
    }

    /**
     * {@inheritDoc}
     * @throws Exception
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ExchangeHelper.sendJsonResponse(exchange, new ContextPayload(service.getContextConfig()));
    }

    public static final class ContextPayload {

        private final Conf config;

        /**
         * Creates a new {@link ContextPayload} instance.
         * @param config    the context configuration.
         */
        public ContextPayload(final Conf config) {
            this.config = config;
        }

        public Conf getConfig() {
            return config;
        }
    }
}

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

import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.data.ErrorMessage;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import java.util.Arrays;
import java.util.List;

public class HeadlessHttpHandler implements HttpHandler {

    private final HttpHandler next;

    /**
     * Creates a new {@link HeadlessHttpHandler} instance.
     */
    public HeadlessHttpHandler(final HttpHandler next) {
        this.next = next;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final HttpString method = exchange.getRequestMethod();
        final String path = exchange.getRelativePath();
        if (isOperationRestricted(path, method) && isNotQueryStore(path, method)) {
            ErrorMessage error = new ErrorMessage(
                StatusCodes.FORBIDDEN,
                "Server is running in headless mode -" +
                        " interactive use of the Azkarra server is disabled.",
                path
            );
            ExchangeHelper.sendJsonResponseWithCode(exchange, error, StatusCodes.FORBIDDEN);
            exchange.endExchange();
        } else {
            next.handleRequest(exchange);
        }
    }

    private boolean isOperationRestricted(final String path, final HttpString method) {
        List<HttpString> notAllowed = Arrays.asList(Methods.PUT, Methods.POST, Methods.DELETE);
        return path.startsWith("/api/") && notAllowed.contains(method);
    }

    private boolean isNotQueryStore(final String path, final HttpString method) {
        return !(method.equals(Methods.POST) && path.contains("/stores/"));
    }
}

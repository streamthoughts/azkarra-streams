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
package io.streamthoughts.azkarra.http.security.handler;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

/**
 * This is the final {@link HttpHandler} in the security chain, it's purpose is to act as a
 * barrier at the end of the chain to ensure authenticate is called after the mechanisms have been associated
 * with the context and the constraint checked.
 *
 * @see io.undertow.security.handlers.AuthenticationCallHandler
 */
public class XMLHttpRequestAwareAuthCallHandler implements HttpHandler {

    private static final String X_REQUESTED_WITH_HEADER = "X-Requested-With";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    private final HttpHandler next;

    public XMLHttpRequestAwareAuthCallHandler(final HttpHandler next) {
        this.next = next;
    }

    /**
     * Only allow the request through if successfully authenticated or if authentication is not required.
     *
     * @see io.undertow.server.HttpHandler#handleRequest(io.undertow.server.HttpServerExchange)
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        SecurityContext context = exchange.getSecurityContext();
        if (context.authenticate()) {
            if(!exchange.isComplete()) {
                next.handleRequest(exchange);
            }
        } else {
            // Remove "WWW-Authenticate" header to prevent browser from opening auth popup
            // when the request is sent from a Javascript client.
            final HeaderMap headers = exchange.getRequestHeaders();
            if (headers.contains(X_REQUESTED_WITH_HEADER) &&
                headers.getFirst(X_REQUESTED_WITH_HEADER).equals("XMLHttpRequest")) {
                exchange.getResponseHeaders().remove(WWW_AUTHENTICATE_HEADER);
            }
            exchange.endExchange();
        }
    }

}
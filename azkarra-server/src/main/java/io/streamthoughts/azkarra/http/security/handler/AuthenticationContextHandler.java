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
package io.streamthoughts.azkarra.http.security.handler;

import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContext;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContextHolder;
import io.streamthoughts.azkarra.http.security.auth.SSLAuthenticationContext;
import io.streamthoughts.azkarra.http.security.auth.UndertowAuthenticationContext;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;

import javax.net.ssl.SSLSession;
import java.net.InetAddress;

public class AuthenticationContextHandler implements HttpHandler {

    private final SecurityMechanism securityMechanism;
    private final HttpHandler next;

    /**
     * Creates a new {@link AuthenticationContextHandler} instance.
     *
     * @param next  the next {@link HttpHandler} in the chain.
     */
    public AuthenticationContextHandler(final SecurityMechanism securityMechanism,
                                        final HttpHandler next) {
        this.securityMechanism = securityMechanism;
        this.next = next;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        final SecurityContext securityContext = exchange.getSecurityContext();

        try {
            ServerConnection connection = exchange.getConnection();
            final InetAddress clientAddress = exchange.getSourceAddress().getAddress();
            final SSLSession sslSession = connection.getSslSession();

            final AuthenticationContext ac;
            if (sslSession != null) {
                ac = new SSLAuthenticationContext(securityMechanism, clientAddress, securityContext, sslSession);
            } else {
                ac = new UndertowAuthenticationContext(securityMechanism, clientAddress, securityContext);
            }
            AuthenticationContextHolder.setAuthenticationContext(ac);
           next.handleRequest(exchange);
       } finally {
            AuthenticationContextHolder.clearContext();
       }
    }
}

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
package io.streamthoughts.azkarra.http.security.authorizer;

import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.http.security.UnauthorizedAccessException;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContext;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContextHolder;
import io.streamthoughts.azkarra.http.security.auth.AzkarraPrincipalBuilder;
import io.streamthoughts.azkarra.http.security.auth.GrantedAuthority;
import io.streamthoughts.azkarra.http.security.auth.UserDetails;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.net.InetAddress;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class AuthorizationHandler implements HttpHandler {

    private final HttpHandler next;

    private final AuthorizationManager authorizationManager;

    private final AzkarraPrincipalBuilder principalBuilder;

    /**
     * Creates a new {@link AuthorizationHandler} instance.
     *
     * @param next                  the {@link HttpHandler} to forward authorized request.
     * @param authorizationManager  the {@link AuthorizationManager} instance.
     * @param principalBuilder      the {@link AzkarraPrincipalBuilder} instance (may be {@code null}).
     */
    public AuthorizationHandler(final HttpHandler next,
                                final AuthorizationManager authorizationManager,
                                final AzkarraPrincipalBuilder principalBuilder) {
        Objects.requireNonNull(authorizationManager, "authorizationManager cannot be null");
        Objects.requireNonNull(next, "next cannot be null");
        this.next = next;
        this.authorizationManager = authorizationManager;
        this.principalBuilder = principalBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final HttpResource httpResource = new HttpResource(
                exchange.getResolvedPath(),
                exchange.getRequestMethod().toString());

        final AuthenticationContext context = AuthenticationContextHolder.getAuthenticationContext();

        final AuthorizationContext authContext = new AuthorizationContext() {

            @Override
            public Principal principal() {
                return principalBuilder != null ?
                    principalBuilder.buildPrincipal(context) :
                    context.getAuthentication().getPrincipal();
            }

            @Override
            public Collection<GrantedAuthority> authorities() {
                UserDetails details = context.getAuthentication().getUserDetails();
                if (details != null) {
                    return details.allGrantedAuthorities();
                }
                return Collections.emptyList();
            }

            @Override
            public InetAddress clientAddress() {
                return context.getClientAddress();
            }

            @Override
            public SecurityMechanism securityMechanism() {
                return context.getSecurityMechanism();
            }

            @Override
            public HttpResource resource() {
                return httpResource;
            }
        };

        AuthorizationResult result = authorizationManager.authenticate(authContext);
        if (result == AuthorizationResult.ALLOWED) {
            next.handleRequest(exchange);
        } else {
            throw new UnauthorizedAccessException("Access not authorize for this resource");
        }
    }
}

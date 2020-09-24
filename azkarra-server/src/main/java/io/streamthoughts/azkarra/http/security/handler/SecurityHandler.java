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

import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.http.security.auth.AzkarraPrincipalBuilder;
import io.streamthoughts.azkarra.http.security.authorizer.AuthorizationHandler;
import io.streamthoughts.azkarra.http.security.authorizer.AuthorizationManager;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SecurityHandler.
 */
public class SecurityHandler implements HttpHandler {

    private List<Pattern> authenticatedRegexMatchers;
    private final IdentityManager idm;
    private HttpHandler securityHandler;
    protected final HttpHandler next;

    private final AuthenticationMechanism authenticationMechanism;
    private final SecurityMechanism securityMechanism;
    private AuthorizationManager authorizationManager;

    private AzkarraPrincipalBuilder principalBuilder;

    /**
     * Creates a new {@link SecurityHandler} instance.
     */
    SecurityHandler(final IdentityManager idm,
                    final AuthorizationManager authorizationManager,
                    final AuthenticationMechanism authenticationMechanism,
                    final SecurityMechanism securityMechanism,
                    final AzkarraPrincipalBuilder principalBuilder,
                    final HttpHandler next,
                    final List<Pattern> authenticatedRegexMatchers) {
        this.idm = idm;
        this.next = next;
        this.authorizationManager = authorizationManager;
        this.authenticationMechanism = authenticationMechanism;
        this.securityMechanism = securityMechanism;
        this.authenticatedRegexMatchers = authenticatedRegexMatchers;
        this.principalBuilder = principalBuilder;
        this.securityHandler = buildSecurityChain(next);
    }

    private HttpHandler buildSecurityChain(final HttpHandler next) {
        HttpHandler handler = new AuthorizationHandler(next, authorizationManager, principalBuilder);
        handler = new XMLHttpRequestAwareAuthCallHandler(handler);
        handler = new AuthenticationContextHandler(securityMechanism, handler);
        handler = new AuthenticationConstraintHandler(handler);
        final List<AuthenticationMechanism> mechanisms = Collections.singletonList(authenticationMechanism);
        handler = new AuthenticationMechanismsHandler(handler, mechanisms);
        handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, idm, handler);
        return handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (isAuthenticationRequired(exchange)) {
            securityHandler.handleRequest(exchange);
        } else {
            next.handleRequest(exchange);
        }
    }

    public SecurityMechanism getSecurityMechanism() {
        return securityMechanism;
    }

    private boolean isAuthenticationRequired(final HttpServerExchange exchange) {
        final String path = exchange.getRelativePath();
        return authenticatedRegexMatchers
            .stream()
            .anyMatch(p -> p.matcher(path).matches());
    }
}

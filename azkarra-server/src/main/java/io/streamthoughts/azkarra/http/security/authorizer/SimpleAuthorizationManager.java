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
package io.streamthoughts.azkarra.http.security.authorizer;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.http.security.SecurityConfig;
import io.streamthoughts.azkarra.http.security.auth.GrantedAuthority;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SimpleAuthorizationManager.
 */
public class SimpleAuthorizationManager implements AuthorizationManager, Configurable {

    private static final String AUTHORIZE_ANY_ROLE = "*";

    private List<String> roles;
    private List<String> restricted;

    private boolean authorizeAll = false;

    /**
     * Creates a new {@link SimpleAuthorizationManager} instance.
     */
    public SimpleAuthorizationManager() {
        this.roles = Collections.emptyList();
        this.restricted = Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        SecurityConfig config = new SecurityConfig(configuration);
        if (config.getAuthenticationRoles().equals(AUTHORIZE_ANY_ROLE)) {
            authorizeAll = true;
        }
        roles = splitRoles(config.getAuthenticationRoles());
        restricted = splitRoles(config.getAuthenticationRestricted());
    }

    private List<String> splitRoles(final String authenticationRoles) {
        return Arrays
                .stream(authenticationRoles.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorizationResult authenticate(final AuthorizationContext context) {
        if (authorizeAll || hasRole(roles, context)) {
            HttpResource resource = context.resource();
            return hasRole(restricted, context) && isWriteAccess(resource) ?
                    AuthorizationResult.DENIED : AuthorizationResult.ALLOWED;
        } else {
            return AuthorizationResult.DENIED;
        }
    }

    private static boolean isWriteAccess(final HttpResource resource) {
        String method = resource.httpMethod();
        return method.equals("POST") || method.equals("PUT") || method.equals("DELETE");
    }

    private static boolean hasRole(final List<String> roles, final AuthorizationContext context) {
        String principal = context.principal().getName();
        if (roles.contains(principal))
            return true;

        List<String> userRoles = context.authorities()
                .stream()
                .map(GrantedAuthority::get)
                .collect(Collectors.toList());

        for (final String role : userRoles) {
            if (roles.contains(role))
                return true;
        }
        return false;
    }
}

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

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.components.NoSuchComponentException;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.errors.ConfException;
import io.streamthoughts.azkarra.http.security.AzkarraIdentityManager;
import io.streamthoughts.azkarra.http.security.SecurityConfig;
import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.http.security.auth.Authenticator;
import io.streamthoughts.azkarra.http.security.auth.AzkarraPrincipalBuilder;
import io.streamthoughts.azkarra.http.security.auth.BasicAuthenticator;
import io.streamthoughts.azkarra.http.security.auth.CertClientAuthenticator;
import io.streamthoughts.azkarra.http.security.auth.UsersIdentityManager;
import io.streamthoughts.azkarra.http.security.authorizer.AuthorizationManager;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.security.impl.ClientCertAuthenticationMechanism;
import io.undertow.server.HttpHandler;

import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Default factory class for building a new {@link SecurityHandler} object.
 */
public class SecurityHandlerFactory {

    private final AzkarraContext context;

    /**
     * Creates a new {@link AzkarraContext} instance.
     *
     * @param context   the {@link AzkarraContext} instance.
     */
    public SecurityHandlerFactory(final AzkarraContext context) {
        this.context = context;
    }

    /**
     * Creates a new {@link SecurityHandler} for the specified configuration.
     *
     * @param securityConfig    the {@link SecurityConfig}.
     * @param handler           the {@link HttpHandler}.
     *
     * @return                  a new {@link SecurityHandler} object.
     */
    public SecurityHandler make(final SecurityConfig securityConfig,
                                final HttpHandler handler) {
        final String authMechanism = securityConfig.getAuthenticationMechanism();
        try {
            final SecurityMechanism mechanism = SecurityMechanism.valueOf(authMechanism);

            final Authenticator authenticator;
            final AuthenticationMechanism authenticationMechanism;

            switch (mechanism) {
                case BASIC_AUTH:
                    final String realm = securityConfig.getAuthenticationRealm();
                    final boolean silent = securityConfig.isBasicAuthenticationSilent();
                    authenticationMechanism = new BasicAuthenticationMechanism(realm, "BASIC", silent);
                    authenticator = new BasicAuthenticator(realm);
                    UsersIdentityManager idm = getUserIdentityManagerOrNull(securityConfig);
                    if (idm != null)
                        ((BasicAuthenticator)authenticator).setUserIdentityManager(idm);
                    break;

                case CLIENT_CERT_AUTH:
                    authenticationMechanism = new ClientCertAuthenticationMechanism();
                    authenticator = new CertClientAuthenticator();
                    break;

                default:
                    throw new AzkarraException("Unknown security mechanism : " + mechanism);
            }

            return new SecurityHandler(
                new AzkarraIdentityManager(authenticator),
                getAuthorizationManager(securityConfig),
                authenticationMechanism,
                mechanism,
                getPrincipalBuilderOrNull(securityConfig),
                handler,
                Collections.singletonList(Pattern.compile("^/api/.*"))
            );

        } catch (IllegalArgumentException e) {
            throw new ConfException("Authentication method not supported : '" + authMechanism + "'");
        }
    }

    private AuthorizationManager getAuthorizationManager(final SecurityConfig securityConfig) {
        AuthorizationManager authorizationManager;
        try {
            authorizationManager = context.getComponent(AuthorizationManager.class);
        } catch (NoSuchComponentException e) {
            authorizationManager = securityConfig.getAuthorizationManager();
        }
        return authorizationManager;
    }

    private UsersIdentityManager getUserIdentityManagerOrNull(final SecurityConfig securityConfig) {
        UsersIdentityManager usersIdentityManager;
        try {
            usersIdentityManager = context.getComponent(UsersIdentityManager.class);
        } catch (NoSuchComponentException e) {
            usersIdentityManager = securityConfig.getUserIdentityManager();
        }
        return usersIdentityManager;
    }

    private AzkarraPrincipalBuilder getPrincipalBuilderOrNull(final SecurityConfig securityConfig) {
        AzkarraPrincipalBuilder principalBuilder;
        try {
            principalBuilder = context.getComponent(AzkarraPrincipalBuilder.class);
        } catch (NoSuchComponentException e) {
            principalBuilder = securityConfig.getAuthenticationPrincipalBuilder();
        }
        return principalBuilder;
    }

}

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
package io.streamthoughts.azkarra.http.security.auth;

import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.undertow.security.api.SecurityContext;

import java.net.InetAddress;
import java.util.Objects;

public class UndertowAuthenticationContext implements AuthenticationContext {

    private io.undertow.security.api.SecurityContext securityContext;

    private final InetAddress clientAddress;

    private final SecurityMechanism securityMechanism;

    private Authentication authentication;

    /**
     * Creates a new {@link UndertowAuthenticationContext} instance.
     *
     * @param clientAddress     the {@link InetAddress} of the client.
     * @param securityContext   the undertow {@link SecurityContext} instance.
     */
    public UndertowAuthenticationContext(final SecurityMechanism securityMechanism,
                                         final InetAddress clientAddress,
                                         final SecurityContext securityContext) {
        this.securityContext = Objects.requireNonNull(securityContext, "security context cannot be null");
        this.clientAddress = Objects.requireNonNull(clientAddress, "clientAddress cannot be null");
        this.securityMechanism = Objects.requireNonNull(securityMechanism, "securityMechanism cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityMechanism getSecurityMechanism() {
        return securityMechanism;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetAddress getClientAddress() {
        return clientAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Authentication getAuthentication() {
        if (authentication == null) {
            authentication = new EmptyAuthentication();
        }
        return authentication;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAuthentication(final Authentication authentication) {
        this.authentication = authentication;
    }

    public io.undertow.security.api.SecurityContext unwrap() {
        return securityContext;
    }

    private static class EmptyAuthentication extends AbstractAuthentication<Credentials> {

        /**
         * Creates a new {@link AbstractAuthentication} instance.
         */
        EmptyAuthentication() {
            super(null, null);
        }
    }
}

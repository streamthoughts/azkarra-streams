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

import java.security.Principal;

public abstract class AbstractAuthentication<T extends Credentials> implements Authentication {

    private final T credentials;
    private final Principal principal;

    private volatile boolean isAuthenticated = false;

    /**
     * Creates a new {@link AbstractAuthentication} instance.
     *
     * @param principal    the {@link Principal} of the user to authenticate.
     * @param credentials  the {@link Credentials} of the user to authenticate.
     */
    public AbstractAuthentication(final Principal principal,
                                  final T credentials) {
        this.credentials = credentials;
        this.principal = principal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getCredentials() {
        return credentials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAuthenticated(final boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }
}

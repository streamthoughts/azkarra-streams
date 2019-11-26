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
package io.streamthoughts.azkarra.http.security;

import io.streamthoughts.azkarra.http.security.auth.Credentials;
import io.streamthoughts.azkarra.http.security.auth.GrantedAuthority;
import io.streamthoughts.azkarra.http.security.auth.UserDetails;
import io.undertow.security.idm.Account;

import java.security.Principal;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AzkarraAccount.
 */
public class AzkarraAccount implements Account {

    private final Principal principal;
    private final Credentials credentials;
    private final UserDetails userDetails;

    /**
     * Creates a new {@link AzkarraAccount} instance.
     *
     * @param principal     the {@link Principal} instance.
     * @param credentials   the {@link Credentials} instance.
     * @param userDetails   the {@link UserDetails} instance.
     */
    public AzkarraAccount(final Principal principal,
                          final Credentials credentials,
                          final UserDetails userDetails) {
        Objects.requireNonNull(principal, "principal cannot be null");
        Objects.requireNonNull(credentials, "credentials cannot be null");
        this.principal = principal;
        this.credentials = credentials;
        this.userDetails = userDetails;
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
    public Set<String> getRoles() {
        if (userDetails == null) {
            return Collections.emptySet();
        }
        return userDetails
            .allGrantedAuthorities()
            .stream()
            .map(GrantedAuthority::get)
            .collect(Collectors.toSet());
    }

    public UserDetails userDetails() {
        return userDetails;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AzkarraAccount)) return false;
        AzkarraAccount that = (AzkarraAccount) o;
        return Objects.equals(principal, that.principal) &&
                Objects.equals(credentials, that.credentials) &&
                Objects.equals(userDetails, that.userDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(principal, credentials, userDetails);
    }
}

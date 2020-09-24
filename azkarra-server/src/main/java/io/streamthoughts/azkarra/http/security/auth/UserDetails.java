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

import java.util.Collection;
import java.util.Collections;

public class UserDetails {

    private final String name;
    private final PasswordCredentials credentials;
    private final Collection<GrantedAuthority> authorities;

    /**
     * Creates a new {@link UserDetails} instance.
     *
     * @param name         the name of the user.
     * @param credentials  the pas{@link PasswordCredentials} of the user.
     */
    UserDetails(final String name,
                final PasswordCredentials credentials) {
        this(name, credentials, Collections.emptyList());
    }

    /**
     * Creates a new {@link UserDetails} instance.
     *
     * @param name         the name of the user.
     * @param credentials  the pas{@link PasswordCredentials} of the user.
     * @param authorities  the authorities granted to the user.
     */
    public UserDetails(final String name,
                       final PasswordCredentials credentials,
                       final Collection<GrantedAuthority> authorities) {
        this.name = name;
        this.credentials = credentials;
        this.authorities = authorities;
    }

    public String name() {
        return name;
    }

    public PasswordCredentials credentials() {
        return credentials;
    }

    public Collection<GrantedAuthority> allGrantedAuthorities() {
        return authorities;
    }
}

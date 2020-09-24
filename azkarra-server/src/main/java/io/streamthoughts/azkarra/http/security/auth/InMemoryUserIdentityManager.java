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

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.http.security.SecurityConfException;
import io.streamthoughts.azkarra.http.security.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple {@link UsersIdentityManager} implementation backed by a in-memory hash Map.
 */
public class InMemoryUserIdentityManager implements UsersIdentityManager, Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryUserIdentityManager.class);

    private final Map<String, UserDetails> users;

    /**
     * Creates a new {@link InMemoryUserIdentityManager} instance.
     */
    public InMemoryUserIdentityManager() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Creates a new {@link InMemoryUserIdentityManager} instance.
     *
     * @param users the {@link UserDetails}.
     */
    private InMemoryUserIdentityManager(final Map<String, UserDetails> users) {
        this.users = users;
    }

    public void addUsers(final UserDetails user) {
        users.put(user.name(), user);
    }

    public UserDetails deleteUsersByName(final String name) {
        return users.remove(name);
    }

    public Set<String> getUsers() {
        return new HashSet<>(this.users.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDetails findUserByName(final String name) {
        return users.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        LOG.debug("Configuring user identities from props: '{}'", SecurityConfig.REST_AUTHENTICATION_USERS_CONFIG);
        SecurityConfig securityConfig = new SecurityConfig(configuration);
        final String[] users = securityConfig.getAuthenticationUsers().split(",");
        Arrays.stream(users)
            .map(String::trim)
            .forEach(user -> {
                String[] upwd = user.split(":", 2);
                if (upwd.length < 2) {
                    throw new SecurityConfException(
                        "Invalid config for '"
                        + SecurityConfig.REST_AUTHENTICATION_USERS_CONFIG
                        + "', Expecting <user>:<password>."
                    );
                }
                addUsers(new UserDetails(upwd[0], PasswordCredentials.get(upwd[1])));
                LOG.debug("Add user credentials for '{}'", upwd[0]);
        });
        if (users.length == 0) {
            LOG.debug("No user configured");
        }
    }
}

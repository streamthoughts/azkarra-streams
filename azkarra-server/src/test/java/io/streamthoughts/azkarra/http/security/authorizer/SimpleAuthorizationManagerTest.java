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

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.http.ServerConfig;
import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.http.security.auth.BasicUserPrincipal;
import io.streamthoughts.azkarra.http.security.auth.GrantedAuthority;
import io.streamthoughts.azkarra.http.security.auth.RoleGrantedAuthority;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

public class SimpleAuthorizationManagerTest {

    private SimpleAuthorizationManager manager = new SimpleAuthorizationManager();

    @BeforeEach
    public void setUp() {
        manager = new SimpleAuthorizationManager();
    }

    @Test
    public void shouldAuthorizeAnyRoleGivenWildcard() {
        Conf conf = ServerConfig.newBuilder()
            .setAuthenticationRoles("*")
            .setAuthenticationRestricted("")
            .build();

        manager.configure(conf);

        AuthorizationResult authenticate = manager.authenticate(newAuthorization("any", "any"));
        Assertions.assertEquals(AuthorizationResult.ALLOWED, authenticate);
    }

    @Test
    public void shouldAuthorizeUserGivenKnownPrincipal() {
        Conf conf = ServerConfig.newBuilder()
                .setAuthenticationRoles("user")
                .setAuthenticationRestricted("")
                .build();

        manager.configure(conf);

        AuthorizationResult authenticate = manager.authenticate(newAuthorization("user", null));
        Assertions.assertEquals(AuthorizationResult.ALLOWED, authenticate);
    }

    @Test
    public void shouldAuthorizeUserGivenKnownRole() {
        Conf conf = ServerConfig.newBuilder()
                .setAuthenticationRoles("Dev")
                .setAuthenticationRestricted("")
                .build();

        manager.configure(conf);

        AuthorizationResult authenticate = manager.authenticate(newAuthorization("user", "Dev"));
        Assertions.assertEquals(AuthorizationResult.ALLOWED, authenticate);
    }

    @Test
    public void shouldNotAuthorizeUserGivenUnknownRole() {
        Conf conf = ServerConfig.newBuilder()
                .setAuthenticationRoles("Admin")
                .build();
        manager.configure(conf);

        AuthorizationResult authenticate = manager.authenticate(
                newAuthorization("user", "Dev")
        );
        Assertions.assertEquals(AuthorizationResult.DENIED, authenticate);
    }

    @Test
    public void shouldNotAuthorizeUserGivenRestrictedRole() {
        Conf conf = ServerConfig.newBuilder()
                .setAuthenticationRoles("Dev")
                .setAuthenticationRestricted("Dev")
                .build();
        manager.configure(conf);

        AuthorizationResult authenticate = manager.authenticate(
                newAuthorization("user", "Dev")
        );
        Assertions.assertEquals(AuthorizationResult.DENIED, authenticate);
    }

    private AuthorizationContext newAuthorization(final String principal, final String role) {
        return new AuthorizationContext() {
            @Override
            public Principal principal() {
                return new BasicUserPrincipal(principal);
            }

            @Override
            public Collection<GrantedAuthority> authorities() {
                if (role != null) {
                    return Collections.singletonList(new RoleGrantedAuthority(role));
                } else {
                    return Collections.emptyList();
                }
            }

            @Override
            public InetAddress clientAddress() {
                return null;
            }

            @Override
            public SecurityMechanism securityMechanism() {
                return null;
            }

            @Override
            public HttpResource resource() {
                return new HttpResource("/", "POST");
            }
        };
    }

}
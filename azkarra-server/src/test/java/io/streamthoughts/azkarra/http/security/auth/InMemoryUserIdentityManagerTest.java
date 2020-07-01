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
package io.streamthoughts.azkarra.http.security.auth;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.http.security.SecurityConfException;
import io.streamthoughts.azkarra.http.security.SecurityConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryUserIdentityManagerTest {

    @Test
    public void shouldThrowSecurityExceptionGivenInvalidConfig() {
        final InMemoryUserIdentityManager manager = new InMemoryUserIdentityManager();
        Assertions.assertThrows(SecurityConfException.class, () ->
            manager.configure(Conf.of(SecurityConfig.REST_AUTHENTICATION_USERS_CONFIG, "alice"))
        );
    }

    @Test
    public void shouldConfigureUsersGivenBothPlaintextAndHashedCredentials() {
        final InMemoryUserIdentityManager manager = new InMemoryUserIdentityManager();

        manager.configure(
            Conf.of(SecurityConfig.REST_AUTHENTICATION_USERS_CONFIG,
            "alice:password,admin:MD5:"+ "5f4dcc3b5aa765d61d8327deb882cf99"
        ));

        assertEquals(2, manager.getUsers().size());

        final UserDetails userAdmin = manager.findUserByName("admin");
        assertNotNull(userAdmin);
        assertTrue(userAdmin.credentials().verify("password"));

        final UserDetails userAlice = manager.findUserByName("alice");
        assertNotNull(userAlice);
        assertTrue(userAlice.credentials().verify("password"));
    }
}
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

import io.streamthoughts.azkarra.http.security.jaas.spi.DefaultAuthenticationCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.Principal;

/**
 * BasicAuthenticator.
 */
public class BasicAuthenticator implements Authenticator {

    private static final Logger LOG = LoggerFactory.getLogger(BasicAuthenticator.class);

    private final String realm;

    private UsersIdentityManager idm;

    /**
     * Creates a new {@link BasicAuthenticator} instance.
     *
     * @param realm the realm name to authenticate user.
     */
    public BasicAuthenticator(final String realm) {
        this.realm = realm;
    }

    public void setUserIdentityManager(final UsersIdentityManager idm) {
        this.idm = idm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Authentication authenticate(final Principal principal,
                                       final Credentials credentials) {

        PasswordCredentials password = (PasswordCredentials) credentials;

        // Create a default authentication instance.
        UsernamePasswordAuthentication authentication = new UsernamePasswordAuthentication(principal, password);

        final DefaultAuthenticationCallbackHandler callbackHandler = new DefaultAuthenticationCallbackHandler();

        LoginContext loginContext = null;
        try {
            loginContext = new LoginContext(realm, callbackHandler);
            authentication = new JAASAuthentication(principal, password, loginContext);
            callbackHandler.setAuthentication(authentication);
        } catch (LoginException e) {
            /* ignore */
        }

        if (loginContext != null) {
            try {
                loginContext.login();
            } catch (LoginException e) {
                LOG.error("Failed to authenticate user using " +
                        "LoginContext for realm '" + realm + "' : "+ e.getMessage());
            }
        }

        // attempt to fallback
        if (!authentication.isAuthenticated() && idm != null) {
            final UserDetails userDetails = idm.findUserByName(principal.getName());
            if (userDetails != null) {
                final PasswordCredentials expected = userDetails.credentials();
                // Check password
                if (expected.verify(password)) {
                    authentication.setUserDetails(userDetails);
                    authentication.setAuthenticated(true);
                }
            }
        }
        return authentication;
    }
}

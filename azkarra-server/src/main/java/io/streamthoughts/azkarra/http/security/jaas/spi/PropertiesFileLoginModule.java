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
package io.streamthoughts.azkarra.http.security.jaas.spi;

import io.streamthoughts.azkarra.http.security.auth.BasicRolePrincipal;
import io.streamthoughts.azkarra.http.security.auth.BasicUserPrincipal;
import io.streamthoughts.azkarra.http.security.auth.PasswordCredentials;
import io.streamthoughts.azkarra.http.security.auth.PropertiesFileUsersIdentityManager;
import io.streamthoughts.azkarra.http.security.auth.UserDetails;
import io.streamthoughts.azkarra.http.security.auth.UsernamePasswordAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This LoginModule imports a user's Principal and credentials information (BasicUserPrincipal,
 * BasicRolePrincipal, UserPasswordCredentials) and associates them with the current Subject.
 *
 * This LoginModule recognizes the debug option. If set to true in the login Configuration, debug messages will
 * be output at the INFO level.
 */
public class PropertiesFileLoginModule implements LoginModule {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesFileLoginModule.class);

    private static final Map<String, PropertiesFileUsersIdentityManager> USERS_MANAGERS = new HashMap<>();

    private static final String DEFAULT_FILE_NAME = "server.password";
    private static final String FILE_OPTION = "file";
    private static final String RELOAD_INTERVAL_OPTION = "reloadInterval";
    private static final String RELOAD_OPTION = "reload";
    private static final String DEBUG_OPTION = "debug";

    private Subject subject;
    private CallbackHandler callbackHandler;

    /**
     * Option 'file' - The file path contains the user's principal to load.
     */
    private String file;
    /**
     * Option 'reloadInterval' - The refresh internal, in second, to reload the file.
     */
    private Duration refreshInterval;
    /**
     * Option 'reload' - Enable file reload.
     */
    private boolean reload;

    /**
     * Option 'debug' - Enable LoginModuleDebug
     */
    private boolean debug = false;

    private JAASUserDetails current;

    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    private UsernamePasswordAuthentication authentication;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final Subject subject,
                           final CallbackHandler callbackHandler,
                           final Map<String, ?> sharedState,
                           final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;

        parseOptions(options);

        if (!USERS_MANAGERS.containsKey(file)) {
            if (debug) {
                LOG.info("Creating a PropertiesFileUsersIdentityManager for: file='" +
                    file + "', refreshInterval='" + refreshInterval + "'");
            }
            final PropertiesFileUsersIdentityManager manager = new PropertiesFileUsersIdentityManager(file, debug);
            if (reload) {
                manager.startAutoReload(refreshInterval);
            }
            USERS_MANAGERS.put(file, manager);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean login() throws LoginException {

        if (callbackHandler == null) {
            throw new LoginException("no callback handler");
        }
        try {

            AuthenticationCallback<UsernamePasswordAuthentication> callback = new AuthenticationCallback<>();
            callbackHandler.handle(new Callback[]{callback});

            authentication = callback.getAuthentication();

            final String username = authentication.getPrincipal().getName();
            mayLog("attempting to login user '" + username + "' using properties file '" + file + "'");

            UserDetails userDetails = getUserDetails(username);
            if (userDetails != null) {
                final PasswordCredentials expected = userDetails.credentials();
                final PasswordCredentials provided = authentication.getCredentials();

                if (expected.verify(provided)) {
                    current = new JAASUserDetails(userDetails);
                    authentication.setUserDetails(userDetails);
                    authentication.setAuthenticated(true);
                    succeeded = true;
                    if (debug) {
                        mayLog("user '" + username + "' have been successfully logged in.");
                    }
                }
            }
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }

        return succeeded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean commit() throws LoginException {
        commitSucceeded = false;
        if (!succeeded) {
            if (debug) {
                mayLog("did not add any Principals to Subject because own authentication failed.");
            }
            current = null;
            authentication = null;
        } else if (subject.isReadOnly()) {
            commitSucceeded = false;
            throw new LoginException("commit Failed: Subject is Readonly");
        } else {
            current.associate(subject);
            commitSucceeded = true;
        }
        return commitSucceeded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean abort() throws LoginException {
        mayLog("aborted authentication attempt");
        if (!succeeded) {
            return false;
        } else if (!commitSucceeded) {
            succeeded = false;
            current = null;
            authentication = null;
        } else {
            logout();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean logout() throws LoginException {
        if (subject.isReadOnly()) {
            throw new LoginException("logout Failed: Subject is Readonly");
        }
        current.disassociate(subject);
        current = null;
        succeeded = false;
        commitSucceeded = false;
        authentication.setAuthenticated(false);
        authentication = null;
        mayLog("logged out Subject");
        return true;
    }

    private UserDetails getUserDetails(final String username) {
        return USERS_MANAGERS.get(file).findUserByName(username);
    }

    private void parseOptions(final Map<String, ?> options) {
        file = options.containsKey(FILE_OPTION) ?
                (String)options.get(FILE_OPTION) : DEFAULT_FILE_NAME;

        refreshInterval = options.containsKey(RELOAD_INTERVAL_OPTION) ?
                Duration.ofSeconds(Long.parseLong((String)options.get(RELOAD_INTERVAL_OPTION)))
                : Duration.ofMinutes(5);

        debug = "true".equalsIgnoreCase((String)options.get(DEBUG_OPTION));
        reload = "true".equalsIgnoreCase((String)options.get(RELOAD_OPTION));
    }

    private static class JAASUserDetails {

        private UserDetails userDetails;
        private List<BasicRolePrincipal> roles;
        private BasicUserPrincipal principal;
        private PasswordCredentials credentials;

        JAASUserDetails(final UserDetails userDetails) {
            this.userDetails = userDetails;
        }

        private void associate(final Subject subject) {
            principal = new BasicUserPrincipal(userDetails.name());
            roles = userDetails.allGrantedAuthorities()
                    .stream()
                    .map(a -> new BasicRolePrincipal(a.get()))
                    .collect(Collectors.toList());
            credentials = userDetails.credentials();

            subject.getPrincipals().add(principal);
            subject.getPrincipals().addAll(roles);
            subject.getPrivateCredentials().add(credentials);
        }

        private void disassociate(final Subject subject) {
            subject.getPrincipals().remove(principal);
            subject.getPrincipals().removeAll(roles);
            subject.getPrivateCredentials().remove(credentials);
        }
    }

    private void mayLog(final String message)  {
        if (debug) {
            LOG.info("[PropertiesFileLoginModule]: " + message);
        }
    }
}

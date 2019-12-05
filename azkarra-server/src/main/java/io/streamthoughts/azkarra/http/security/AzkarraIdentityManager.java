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

import io.streamthoughts.azkarra.http.security.auth.Authentication;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContext;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContextHolder;
import io.streamthoughts.azkarra.http.security.auth.Authenticator;
import io.streamthoughts.azkarra.http.security.auth.BasicUserPrincipal;
import io.streamthoughts.azkarra.http.security.auth.Credentials;
import io.streamthoughts.azkarra.http.security.auth.PlainPasswordCredentials;
import io.streamthoughts.azkarra.http.security.auth.X509CertificateCredentials;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.idm.X509CertificateCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.security.cert.X509Certificate;

/**
 * BasicMapIdentityManager.
 */
public class AzkarraIdentityManager implements IdentityManager {

    private static final Logger LOG = LoggerFactory.getLogger(AzkarraIdentityManager.class);

    private final Authenticator authenticator;

    /**
     * Creates a new {@link AzkarraIdentityManager} instance.
     *
     * @param authenticator the {@link Authenticator} instance.
     */
    public AzkarraIdentityManager(final Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Account verify(final Account account) {
        return account;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Account verify(final String id, final Credential credential) {
        return verifyCredential(id, credential);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Account verify(final Credential credential) {
        return verifyCredential(null, credential);
    }

    private Account verifyCredential(final String id, final Credential credential) {

        final AuthenticationContext context = AuthenticationContextHolder.getAuthenticationContext();

        if (isPasswordCredential(credential)) {
            final Principal principal = new BasicUserPrincipal(id);
            char[] password = ((PasswordCredential) credential).getPassword();
            final Credentials credentials = new PlainPasswordCredentials(String.valueOf(password));

            return authenticate(context, principal, credentials);
        }

        if (isX509CertificateCredential(credential)) {
            X509Certificate certificate = ((X509CertificateCredential) credential).getCertificate();
            final Principal principal = certificate.getSubjectX500Principal();
            final Credentials credentials = new X509CertificateCredentials(certificate);

            return authenticate(context, principal, credentials);
        }
        LOG.error("Cannot verify authentication for credential type '" + credential.getClass().getName() + "'");
        return null;
    }

    private Account authenticate(final AuthenticationContext context,
                                 final Principal principal,
                                 final Credentials credentials) {
        final Authentication authentication = authenticator.authenticate(principal, credentials);
        context.setAuthentication(authentication);

        if (authentication.isAuthenticated()) {
            return new AzkarraAccount(
                authentication.getPrincipal(),
                authentication.getCredentials(),
                authentication.getUserDetails()
            );
        }

        return null;
    }

    private static boolean isX509CertificateCredential(final Credential credential) {
        return credential instanceof X509CertificateCredential;
    }

    private static boolean isPasswordCredential(final Credential credential) {
        return credential instanceof PasswordCredential;
    }
}

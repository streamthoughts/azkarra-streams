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

import java.security.Principal;

public class CertClientAuthenticator implements Authenticator {

    /**
     * {@inheritDoc}
     */
    @Override
    public Authentication authenticate(final Principal principal,
                                       final Credentials credentials) {

        final X509CertificateCredentials x509Credentials = (X509CertificateCredentials) credentials;
        final SSLClientAuthentication authentication = new SSLClientAuthentication(principal, x509Credentials);
        authentication.setAuthenticated(true);
        return authentication;
    }
}

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

import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * X509CertificateCredentials.
 */
public class X509CertificateCredentials implements Credentials {

    private final X509Certificate certificate;

    /**
     * Creates a new {@link X509Certificate} instance.
     *
     * @param certificate   the {@link X509Certificate} instance.
     */
    public X509CertificateCredentials(final X509Certificate certificate) {
        Objects.requireNonNull(certificate, "certification cannot be null");
        this.certificate = certificate;
    }

    public X509Certificate getX509Certificate() {
        return certificate;
    }

}

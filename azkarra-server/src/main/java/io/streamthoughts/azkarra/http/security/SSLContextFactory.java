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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;

/**
 * Default class for retrieving a new {@link SSLContext} instance.
 */
public class SSLContextFactory {

    private static final String PROTOCOL_TLS = "TLS";

    private final SecurityConfig conf;

    private SSLContext sslContext;

    private TrustManager[] trustManagers;

    private KeyManager[] keyManagers;

    /**
     * Creates a new {@link SSLContextFactory} instance.
     *
     * @param conf  the {@link SecurityConfig} instance.
     */
    public SSLContextFactory(final SecurityConfig conf) {
        Objects.requireNonNull(conf, "conf cannot be null");
        this.conf = conf;
    }

    /**
     * Gets the {@link SSLContext} instance using the specified configuration.
     *
     * @return  a new {@link SSLContext} instance.
     */
    public SSLContext getSSLContext() {
        if (sslContext == null) {
            final KeyStore keyStore = getConfigureKeyStore();
            final KeyStore trustStore = getConfiguredTrustStore();

            final char[] keyPassword = conf.getKeyPassword();
            try {
                final String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();

                buildKeyManagers(keyStore, keyPassword, defaultAlgorithm);
                buildTrustManagers(trustStore, defaultAlgorithm);

                sslContext = SSLContext.getInstance(PROTOCOL_TLS);
                sslContext.init(keyManagers, trustManagers, null);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new InvalidSSLConfException(e);
            }
        }
        return sslContext;
    }

    public X509TrustManager getTrustManager()  {
        buildTrustManagers(getConfiguredTrustStore(), KeyManagerFactory.getDefaultAlgorithm());

        X509TrustManager manager = null;
        for (TrustManager tm : trustManagers) {
            if (tm instanceof X509TrustManager) {
                manager = (X509TrustManager) tm;
                break;
            }
        }
        return manager;
    }

    private KeyStore getConfigureKeyStore() {
        final String keyStoreLocation = conf.getKeystoreLocation();
        final char[] keyStorePassword = conf.getKeystorePassword();
        return loadStore(keyStoreLocation, keyStorePassword, conf.getKeystoreType());
    }

    private KeyStore getConfiguredTrustStore() {
        KeyStore trustStore = null;
        if (conf.getTrustStoreLocation() != null) {
            final String trustStoreLocation = conf.getTrustStoreLocation();
            final char[] trustStorePassword = conf.getTruststorePassword();
            trustStore = loadStore(trustStoreLocation, trustStorePassword, conf.getTruststoreType());
        }
        return trustStore;
    }

    private void buildTrustManagers(final KeyStore trustStore,
                                              final String defaultAlgorithm) {
        if (trustManagers == null) {
            try {
                final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
                // Get default from cacerts if trustStore is null.
                trustManagerFactory.init(trustStore);
                trustManagers =  trustManagerFactory.getTrustManagers();
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                throw new InvalidSSLConfException(e);
            }
        }
    }

    private void buildKeyManagers(final KeyStore keyStore,
                                          final char[] keyPassword,
                                          final String defaultAlgorithm)  {
        if (keyManagers == null) {
            try {
                final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(defaultAlgorithm);
                keyManagerFactory.init(keyStore, keyPassword);
                keyManagers =  keyManagerFactory.getKeyManagers();
            } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
                throw new InvalidSSLConfException(e);
            }
        }
    }

    private static KeyStore loadStore(final String keyStoreLocation,
                                      final char[] keyStorePassword,
                                      final String keyStoreType) {
        try(InputStream is = Files.newInputStream(Paths.get(keyStoreLocation))) {
            try {
                KeyStore ks = KeyStore.getInstance(keyStoreType);
                ks.load(is, keyStorePassword);
                return ks;
            } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
                throw new InvalidSSLConfException(e);
            }
        } catch (IOException e) {
            throw new InvalidSSLConfException("Error while loading KeyStore from '" + keyStoreLocation + "'", e);
        }
    }
}

/*
 * Copyright 2019-2021 StreamThoughts.
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
package io.streamthoughts.azkarra.client;

import io.streamthoughts.azkarra.client.security.SSLContextFactory;
import io.streamthoughts.azkarra.client.security.SSLUtils;
import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.internal.tls.OkHostnameVerifier;
import okhttp3.logging.HttpLoggingInterceptor;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * The {@code HttpClientBuilder} can be used for build a new {@link OkHttpClient} instance.
 */
public class HttpClientBuilder {

    public static final AllowAllHostNameVerifier NO_HOST_NAME_VERIFIER = new AllowAllHostNameVerifier();

    private boolean debugging = false;
    private HttpLoggingInterceptor loggingInterceptor;

    private OkHttpClient httpClient;
    private boolean verifyingSsl = true;
    private KeyManager[] keyManagers = {};
    private TrustManager[] trustManagers = {};
    private SSLContext sslContext;

    /**
     * Helper method to create a new {@link HttpClientBuilder} instance.
     * @return  a new {@link HttpClientBuilder} instance.
     */
    public static HttpClientBuilder newBuilder() {
        return new HttpClientBuilder();
    }

    private HttpClientBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        httpClient = builder.build();
    }

    public HttpClientBuilder verifyingSsl(boolean verifyingSsl) {
        this.verifyingSsl = verifyingSsl;
        applySslConfigs();
        return this;
    }

    public HttpClientBuilder sslContext(final SSLContext sslContext) {
        this.sslContext = sslContext;
        applySslConfigs();
        return this;
    }

    public HttpClientBuilder sslKeyManagers(final KeyManager[] keyManagers) {
        this.keyManagers = keyManagers;
        applySslConfigs();
        return this;
    }

    public HttpClientBuilder sslTrustManagers(final TrustManager[] trustManagers) {
        this.trustManagers = trustManagers;
        applySslConfigs();
        return this;
    }
    public HttpClientBuilder authenticator(final Authenticator authenticator) {
        httpClient = httpClient.newBuilder().authenticator(authenticator).build();
        return this;
    }

    /**
     * Enable/disable debugging for this API client.
     *
     * @param debugging To enable (true) or disable (false) debugging
     * @return          this {@link HttpClientBuilder}
     */
    public HttpClientBuilder debugging(boolean debugging) {
        if (debugging != this.debugging) {
            if (debugging) {
                loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClient = httpClient.newBuilder().addInterceptor(loggingInterceptor).build();
            } else {
                httpClient.interceptors().remove(loggingInterceptor);
                loggingInterceptor = null;
            }
        }
        this.debugging = debugging;
        return this;
    }

    /**
     * Sets the connect timeout (in milliseconds).
     * A value of 0 means no timeout, otherwise values must be between 1 and
     * {@link Integer#MAX_VALUE}.
     *
     * @param connectionTimeout connection timeout in milliseconds
     * @return                  this {@link HttpClientBuilder}
     */
    public HttpClientBuilder connectTimeout(final int connectionTimeout) {
        httpClient = httpClient.newBuilder().connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS).build();
        return this;
    }

    /**
     * Sets the read timeout (in milliseconds).
     * A value of 0 means no timeout, otherwise values must be between 1 and
     * {@link Integer#MAX_VALUE}.
     *
     * @param readTimeout read timeout in milliseconds
     * @return            this {@link HttpClientBuilder}
     */
    public HttpClientBuilder readTimeout(int readTimeout) {
        httpClient = httpClient.newBuilder().readTimeout(readTimeout, TimeUnit.MILLISECONDS).build();
        return this;
    }


    /**
     * Sets the write timeout (in milliseconds).
     * A value of 0 means no timeout, otherwise values must be between 1 and
     * {@link Integer#MAX_VALUE}.
     *
     * @param writeTimeout connection timeout in milliseconds
     * @return             this {@link HttpClientBuilder}
     */
    public HttpClientBuilder writeTimeout(int writeTimeout) {
        httpClient = httpClient.newBuilder().writeTimeout(writeTimeout, TimeUnit.MILLISECONDS).build();
        return this;
    }

    /**
     * Builds the http client.
     * @return            a {@link OkHttpClient} instance.
     */
    public OkHttpClient build() {
        return httpClient
                .newBuilder()
                .build();
    }

    private void applySslConfigs() {
            HostnameVerifier hostnameVerifier = verifyingSsl ? OkHostnameVerifier.INSTANCE : NO_HOST_NAME_VERIFIER;
            SSLContext sslContext = this.sslContext;

            if (sslContext == null) {
                TrustManager[] trustManagers;
                if (!verifyingSsl) {
                    trustManagers = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[]{};
                            }
                        }
                    };
                } else {
                    trustManagers = this.trustManagers;
                }
                sslContext = new SSLContextFactory().getSSLContext(keyManagers, trustManagers);
            }

            httpClient = httpClient.newBuilder()
                .sslSocketFactory(sslContext.getSocketFactory(), SSLUtils.getX509TrustManager(trustManagers))
                .hostnameVerifier(hostnameVerifier)
                .build();
    }

    /**
     * A {@link HostnameVerifier} that accept all certificates.
     */
    public static class AllowAllHostNameVerifier implements HostnameVerifier {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean verify(final String hostname, final SSLSession sslSession) {
            return true;
        }
    }

}

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
package io.streamthoughts.azkarra.http.query;

import io.streamthoughts.azkarra.http.security.AllowAllHostNameVerifier;
import io.streamthoughts.azkarra.http.security.SSLContextFactory;
import io.streamthoughts.azkarra.http.security.SecurityMechanism;
import io.streamthoughts.azkarra.http.security.auth.Authentication;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContext;
import io.streamthoughts.azkarra.http.security.auth.AuthenticationContextHolder;
import io.streamthoughts.azkarra.http.security.auth.PasswordCredentials;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import javax.net.ssl.SSLContext;
import java.util.Objects;

/**
 * Builder class for creating a new {@link HttpRemoteQueryClient} instance.
 */
public class HttpRemoteQueryBuilder {

    private String basePath = "/";

    private SSLContextFactory sslContextFactory;

    private boolean ignoreHostnameVerification = false;

    private boolean enablePasswordAuthentication;

    /**
     * Sets the {@link SSLContextFactory} which is used for initializing HTTP client with SSL.
     *
     * @param sslContextFactory the {@link SSLContextFactory} instance.
     * @return  {@code this}
     */
    public HttpRemoteQueryBuilder setSSLContextFactory(final SSLContextFactory sslContextFactory) {
        Objects.requireNonNull(sslContextFactory, "sslContextFactory cannot be null");
        this.sslContextFactory = sslContextFactory;
        return this;
    }

    /**
     * Sets if hostname verification must be ignore when SSL is enable.
     *
     * @param ignoreHostnameVerification    {@code true} to ignore hostname verification, {@code false} otherwise.
     * @return  {@code this}
     */
    public HttpRemoteQueryBuilder setIgnoreHostnameVerification(final boolean ignoreHostnameVerification) {
        this.ignoreHostnameVerification = ignoreHostnameVerification;
        return this;
    }

    /**
     * Sets the relative URL base path.
     *
     * @param basePath  the relative base path.
     * @return  {@code this}
     */
    public HttpRemoteQueryBuilder setBasePath(final String basePath) {
        Objects.requireNonNull(basePath, "basePath cannot be null");
        this.basePath = ("/" + basePath + "/").replaceAll("//", "/");
        return this;
    }

    /**
     * Sets if authentication is required.
     *
     * @param enablePasswordAuthentication  the {@link PasswordCredentials} supplier.
     * @return  {@code this}
     */
    public HttpRemoteQueryBuilder enablePasswordAuthentication(final boolean enablePasswordAuthentication) {
        this.enablePasswordAuthentication = enablePasswordAuthentication;
        return this;
    }

    /**
     * Builds the {@link HttpRemoteQueryClient} instance.
     *
     * @return  the new {@link HttpRemoteQueryClient} instance.
     */
    public HttpRemoteQueryClient build() {

        String schema = "http";
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (sslContextFactory != null) {
            SSLContext sslContext = sslContextFactory.getSSLContext();
            builder.sslSocketFactory(sslContext.getSocketFactory(), sslContextFactory.getTrustManager());
            schema = "https";
        }

        if (ignoreHostnameVerification) {
            builder.hostnameVerifier(new AllowAllHostNameVerifier());
        }

        if (enablePasswordAuthentication) {
            builder.authenticator(getPasswordAuthenticator());
        }
        OkHttpClient httpClient = builder.build();
        return new HttpRemoteQueryClient(httpClient, new DefaultQueryURLBuilder(schema, basePath));
    }

    private Authenticator getPasswordAuthenticator() {
        return new Authenticator() {
            public Request authenticate(final Route route, final Response response) {

                AuthenticationContext context = AuthenticationContextHolder.getAuthenticationContext();

                SecurityMechanism securityMechanism = context.getSecurityMechanism();

                if (securityMechanism == SecurityMechanism.BASIC_AUTH) {
                    Authentication authentication = context.getAuthentication();
                    final String password = ((PasswordCredentials) authentication.getCredentials()).password();
                    final String username = authentication.getPrincipal().getName();
                    final String basic = Credentials.basic(username, password);
                    return response.request().newBuilder().header("Authorization", basic).build();
                }
                return response.request().newBuilder().build();
            }
        };
    }

    private static class DefaultQueryURLBuilder implements QueryURLBuilder {

        private final String schema;

        private final String basePath;

        /**
         * Creates a new {@link DefaultQueryURLBuilder} instance.
         *
         * @param schema     the http schema.
         * @param basePath   the url base relative path.
         */
        DefaultQueryURLBuilder(final String schema, final String basePath) {
            this.schema = schema;
            this.basePath = basePath;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String buildURL(final String server, final String applicationId, final String store) {
            return schema + "://" + server + basePath + "applications/" + applicationId + "/stores/" + store;
        }
    }
}

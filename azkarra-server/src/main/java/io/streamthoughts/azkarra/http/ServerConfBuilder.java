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
package io.streamthoughts.azkarra.http;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.http.security.SecurityConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * An helper class for build a {@link Conf} used for configuring embedded http-server.
 */
public class ServerConfBuilder {

    private static final String HTTP_PORT_CONFIG                    = "port";
    private static final String HTTP_LISTENER_LISTER_CONFIG         = "listener";

    private final Map<String, Object> configs;

    public static ServerConfBuilder newBuilder() {
        return new ServerConfBuilder();
    }

    /**
     * Creates a new {@link ServerConfBuilder}.
     */
    private ServerConfBuilder() {
        configs = new HashMap<>();
    }

    /**
     * Sets the http server port.
     *
     * @param port  the http port.
     * @return  {@code this}.
     */
    public ServerConfBuilder setPort(final int port) {
        configs.put(HTTP_PORT_CONFIG, port);
        return this;
    }

    /**
     * Sets the http server listener.
     *
     * @param listener  the http listener.
     * @return  {@code this}.
     */
    public ServerConfBuilder setListener(final String listener) {
        configs.put(HTTP_LISTENER_LISTER_CONFIG, listener);
        return this;
    }

    /**
     * Sets the authentication mode.
     *
     * @param method       the authentication method.
     * @return  {@code this}.
     */
    public ServerConfBuilder setAuthenticationMethod(final String method) {
        configs.put(SecurityConfig.REST_AUTHENTICATION_MECHANISM_CONFIG, method);
        return this;
    }

    /**
     * Sets the authentication realm.
     *
     * @param realm         the authentication realM
     * @return  {@code this}.
     */
    public ServerConfBuilder setAuthenticationRealm(final String realm) {
        configs.put(SecurityConfig.REST_AUTHENTICATION_REALM_CONFIG, realm);
        return this;
    }

    /**
     * Sets the authentication roles.
     *
     * @param roles         the authentication roles.
     * @return  {@code this}.
     */
    public ServerConfBuilder setAuthenticationRoles(final String roles) {
        configs.put(SecurityConfig.REST_AUTHENTICATION_ROLES_CONFIG, roles);
        return this;
    }

    /**
     * Sets if the basic authentication must be silent. The server will respond with a
     * 403 Forbidden HTTP response status code instead of a 401 Unauthorized (default is {@code false}).
     *
     * @param silent       is basic authentication must be silent.
     * @return  {@code this}.
     */
    public ServerConfBuilder setBasicSilentAuthentication(final boolean silent) {
        configs.put(SecurityConfig.REST_AUTHENTICATION_BASIC_SILENT_CONFIG, silent);
        return this;
    }

    /**
     * Sets the authentication roles.
     *
     * @param roles         the authentication roles.
     * @return  {@code this}.
     */
    public ServerConfBuilder setAuthenticationRestricted(final String roles) {
        configs.put(SecurityConfig.HTTP_RESTRICTED_ROLES_CONFIG, roles);
        return this;
    }

    /**
     * Sets the authentication users.
     *
     * @param users         the list users to authenticate separated by comma.
     * @return  {@code this}.
     */
    public ServerConfBuilder setAuthenticationUsers(final String users) {
        configs.put(SecurityConfig.REST_AUTHENTICATION_USERS_CONFIG, users);
        return this;
    }

    /**
     * Enables the SSL.
     *
     * @return  {@code this}.
     */
    public ServerConfBuilder setIgnoreSslHostnameVerification(final boolean ignore) {
        configs.put(SecurityConfig.SSL_IGNORE_HOSTNAME_VERIFICATION, ignore);
        return this;
    }

    /**
     * Enables the SSL.
     *
     * @return  {@code this}.
     */
    public ServerConfBuilder enableSsl() {
        configs.put(SecurityConfig.SSL_ENABLE, true);
        return this;
    }
    /**
     * Disables SSL.
     *
     * @return  {@code this}.
     */
    public ServerConfBuilder disableSsl() {
        configs.put(SecurityConfig.SSL_ENABLE, false);
        return this;
    }

    /**
     * Sets the location of the key store file.
     *
     * @param keyStoreLocation the location of the key store file.
     *
     * @return  {@code this}.
     */
    public ServerConfBuilder setKeyStoreLocation(final String keyStoreLocation) {
        configs.put(SecurityConfig.SSL_KEYSTORE_LOCATION, keyStoreLocation);
        return this;
    }

    /**
     * Sets the store password for the key store file.
     *
     * @param keyStorePassword the store password for the key store file.
     * @return  {@code this}.
     */
    public ServerConfBuilder setKeyStorePassword(final String keyStorePassword) {
        configs.put(SecurityConfig.SSL_KEYSTORE_PASSWORD, keyStorePassword);
        return this;
    }

    /**
     * Sets the file format of the key store file.
     *
     * @param keyStoreType     the file format of the key store file.
     * @return  {@code this}.
     */
    public ServerConfBuilder setKeyStoreType(final String keyStoreType) {
        configs.put(SecurityConfig.SSL_TRUSTSTORE_TYPE, keyStoreType);
        return this;
    }

    /**
     * Sets the password of the private key in the key store file.
     *
     * @param keyPassword the password of the private key in the key store file.
     *
     * @return  {@code this}.
     */
    public ServerConfBuilder setKeyPassword(final String keyPassword) {
        configs.put(SecurityConfig.SSL_KEY_PASSWORD_CONFIG, keyPassword);
        return this;
    }

    /**
     * Sets the location of the trust store file.
     *
     * @param trustStoreLocation the location of the trust store file.
     *
     * @return  {@code this}.
     */
    public ServerConfBuilder setTrustStoreLocation(final String trustStoreLocation) {
        configs.put(SecurityConfig.SSL_TRUSTSTORE_LOCATION, trustStoreLocation);
        return this;
    }

    /**
     * Sets the store password for the trust store file.
     *
     * @param trustStorePassword the store password for the trust store file.
     *
     * @return  {@code this}.
     */
    public ServerConfBuilder setTrustStorePassword(final String trustStorePassword) {
        configs.put(SecurityConfig.SSL_TRUSTSTORE_PASSWORD, trustStorePassword);
        return this;
    }

    /**
     * Sets the file format of the key trust file.
     *
     * @param trustStoreType     the file format of the key trust file.
     *
     * @return  {@code this}.
     */
    public ServerConfBuilder setTrustStoreType(final String trustStoreType) {
        configs.put(SecurityConfig.SSL_TRUSTSTORE_TYPE, trustStoreType);
        return this;
    }

    /**
     * Enables the headless mode.
     *
     * @return  {@code this}
     */
    public ServerConfBuilder enableHeadlessMode() {
        configs.put(SecurityConfig.HTTP_HEADLESS_CONFIG, true);
        return this;
    }
    /**
     * Disables the headless mode.
     *
     * @return  {@code this}
     */
    public ServerConfBuilder disableHeadlessMode() {
        configs.put(SecurityConfig.HTTP_HEADLESS_CONFIG, false);
        return this;
    }

    public Conf build() {
        return Conf.with(configs);
    }
}

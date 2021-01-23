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
package io.streamthoughts.azkarra.http;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.http.security.SecurityConfig;
import io.streamthoughts.azkarra.http.security.auth.AzkarraPrincipalBuilder;
import io.streamthoughts.azkarra.http.security.auth.UsersIdentityManager;

import java.util.HashMap;
import java.util.Map;

/**
 * An helper class for build a {@link Conf} used for configuring embedded http-server.
 */
public class ServerConfigBuilder {

    public static final String HTTP_PORT_CONFIG                    = "port";
    public static final String HTTP_LISTENER_LISTER_CONFIG         = "listener";
    public static final String HTTP_ENABLE_UI                      = "enable.ui";
    public static final String HTTP_REST_EXTENSIONS_ENABLE         = "rest.extensions.enable";

    private final Map<String, Object> configs;

    /**
     * Creates a new {@link ServerConfigBuilder}.
     */
    ServerConfigBuilder() {
        this(new HashMap<>());
    }

    /**
     * Creates a new {@link ServerConfigBuilder}.
     */
    ServerConfigBuilder(final Conf configs) {
        this(configs.getConfAsMap());
    }

    /**
     * Creates a new {@link ServerConfigBuilder}.
     */
    ServerConfigBuilder(final Map<String, Object> configs) {
        this.configs = new HashMap<>(configs);
    }

    /**
     * Sets the http server port.
     *
     * @param port  the http port.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setPort(final int port) {
        configs.put(HTTP_PORT_CONFIG, port);
        return this;
    }

    /**
     * Sets the http server listener.
     *
     * @param listener  the http listener.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setListener(final String listener) {
        configs.put(HTTP_LISTENER_LISTER_CONFIG, listener);
        return this;
    }

    /**
     * Sets if the Web UI must be enable.
     *
     * @param enable    {@code true} to enable the Web UI, {@code false} otherwise.
     * @return  {@code this}.
     */
    public ServerConfigBuilder enableUI(final boolean enable) {
        configs.put(HTTP_ENABLE_UI, enable);
        return this;
    }

    /**
     * Sets the authentication mode.
     *
     * @param method       the authentication method.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setAuthenticationMethod(final String method) {
        configs.put(SecurityConfig.REST_AUTHENTICATION_MECHANISM_CONFIG, method);
        return this;
    }

    /**
     * Sets the authentication realm.
     *
     * @param realm         the authentication realM
     * @return  {@code this}.
     */
    public ServerConfigBuilder setAuthenticationRealm(final String realm) {
        configs.put(SecurityConfig.REST_AUTHENTICATION_REALM_CONFIG, realm);
        return this;
    }

    /**
     * Sets the authentication roles.
     *
     * @param roles         the authentication roles.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setAuthenticationRoles(final String roles) {
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
    public ServerConfigBuilder setBasicSilentAuthentication(final boolean silent) {
        configs.put(SecurityConfig.REST_AUTHENTICATION_BASIC_SILENT_CONFIG, silent);
        return this;
    }

    /**
     * Sets the authentication roles.
     *
     * @param roles         the authentication roles.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setAuthenticationRestricted(final String roles) {
        configs.put(SecurityConfig.HTTP_RESTRICTED_ROLES_CONFIG, roles);
        return this;
    }

    /**
     * Sets the {@link UsersIdentityManager} used to get information about user to authenticate.
     *
     * @param cls   the {@link UsersIdentityManager} class.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setUserIdentityManager(final Class<? extends UsersIdentityManager> cls) {
        configs.put(SecurityConfig.HTTP_AUTH_USER_IDENTITY_MANAGER_CLASS_CONFIG, cls.getName());
        return this;
    }

    /**
     * Sets the {@link AzkarraPrincipalBuilder} used to build the principal for an authenticated user.
     *
     * @param cls   the {@link AzkarraPrincipalBuilder} class.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setPrincipalBuilder(final Class<? extends AzkarraPrincipalBuilder> cls) {
        configs.put(SecurityConfig.HTTP_AUTH_PRINCIPAL_BUILDER_CLASS_CONFIG, cls.getName());
        return this;
    }

    /**
     * Sets the authentication users.
     *
     * @param users         the list users to authenticate separated by comma.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setAuthenticationUsers(final String users) {
        configs.put(SecurityConfig.REST_AUTHENTICATION_USERS_CONFIG, users);
        return this;
    }

    /**
     * Enables the SSL.
     *
     * @return  {@code this}.
     */
    public ServerConfigBuilder setIgnoreSslHostnameVerification(final boolean ignore) {
        configs.put(SecurityConfig.SSL_IGNORE_HOSTNAME_VERIFICATION, ignore);
        return this;
    }

    /**
     * Enables the SSL.
     *
     * @return  {@code this}.
     */
    public ServerConfigBuilder enableSsl() {
        configs.put(SecurityConfig.SSL_ENABLE, true);
        return this;
    }
    /**
     * Disables SSL.
     *
     * @return  {@code this}.
     */
    public ServerConfigBuilder disableSsl() {
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
    public ServerConfigBuilder setKeyStoreLocation(final String keyStoreLocation) {
        configs.put(SecurityConfig.SSL_KEYSTORE_LOCATION, keyStoreLocation);
        return this;
    }

    /**
     * Sets the store password for the key store file.
     *
     * @param keyStorePassword the store password for the key store file.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setKeyStorePassword(final String keyStorePassword) {
        configs.put(SecurityConfig.SSL_KEYSTORE_PASSWORD, keyStorePassword);
        return this;
    }

    /**
     * Sets the file format of the key store file.
     *
     * @param keyStoreType     the file format of the key store file.
     * @return  {@code this}.
     */
    public ServerConfigBuilder setKeyStoreType(final String keyStoreType) {
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
    public ServerConfigBuilder setKeyPassword(final String keyPassword) {
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
    public ServerConfigBuilder setTrustStoreLocation(final String trustStoreLocation) {
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
    public ServerConfigBuilder setTrustStorePassword(final String trustStorePassword) {
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
    public ServerConfigBuilder setTrustStoreType(final String trustStoreType) {
        configs.put(SecurityConfig.SSL_TRUSTSTORE_TYPE, trustStoreType);
        return this;
    }

    /**
     * Enables the headless mode.
     *
     * @return  {@code this}
     */
    public ServerConfigBuilder enableHeadlessMode() {
        configs.put(SecurityConfig.HTTP_HEADLESS_CONFIG, true);
        return this;
    }
    /**
     * Disables the headless mode.
     *
     * @return  {@code this}
     */
    public ServerConfigBuilder disableHeadlessMode() {
        configs.put(SecurityConfig.HTTP_HEADLESS_CONFIG, false);
        return this;
    }

    /**
     * Enables support for rest extensions.
     * @see io.streamthoughts.azkarra.api.server.AzkarraRestExtension
     *
     * @return  {@code this}
     */
    public ServerConfigBuilder enableRestExtensions() {
        configs.put(HTTP_REST_EXTENSIONS_ENABLE, true);
        return this;
    }

    /**
     * Disables support for rest extensions.
     * @see io.streamthoughts.azkarra.api.server.AzkarraRestExtension
     *
     * @return  {@code this}
     */
    public ServerConfigBuilder disableRestExtensions() {
        configs.put(HTTP_REST_EXTENSIONS_ENABLE, false);
        return this;
    }

    public ServerConfig build() {
        return new ServerConfig(Conf.of(configs));
    }
}

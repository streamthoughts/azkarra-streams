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

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.config.DelegatingConf;
import io.streamthoughts.azkarra.http.security.auth.AzkarraPrincipalBuilder;
import io.streamthoughts.azkarra.http.security.auth.UsersIdentityManager;
import io.streamthoughts.azkarra.http.security.authorizer.AuthorizationManager;
import io.streamthoughts.azkarra.http.security.authorizer.SimpleAuthorizationManager;

/**
 * The server security confiugration.
 */
public class SecurityConfig extends DelegatingConf {

    public static final String REST_AUTHENTICATION_MECHANISM_CONFIG     = "rest.authentication.mechanism";
    public static final String REST_AUTHENTICATION_REALM_CONFIG         = "rest.authentication.realm";
    public static final String REST_AUTHENTICATION_ROLES_CONFIG         = "rest.authentication.roles";
    public static final String REST_AUTHENTICATION_USERS_CONFIG         = "rest.authentication.users";
    public static final String REST_AUTHENTICATION_BASIC_SILENT_CONFIG  = "rest.authentication.basic.silent";

    public static final String HTTP_AUTH_PRINCIPAL_BUILDER_CLASS_CONFIG = "principal.builder.class";
    public static final String HTTP_AUTHORIZATION_MANAGER_CLASS_CONFIG  = "authorization.manager.class";
    public static final String HTTP_AUTH_USER_IDENTITY_MANAGER_CLASS_CONFIG = "users.identity.manager.class";
    public static final String HTTP_RESTRICTED_ROLES_CONFIG         = "auth.restricted.roles";
    public static final String HTTP_HEADLESS_CONFIG                 = "headless";

    public static final String SSL_ENABLE                           = "ssl.enable";
    public static final String SSL_IGNORE_HOSTNAME_VERIFICATION     = "ssl.ignore.hostname.verification";
    public static final String SSL_TRUSTSTORE_LOCATION              = "ssl.truststore.location";
    public static final String SSL_TRUSTSTORE_PASSWORD              = "ssl.truststore.password";
    public static final String SSL_TRUSTSTORE_TYPE                  = "ssl.truststore.type";
    public static final String SSL_KEYSTORE_LOCATION                = "ssl.keystore.location";
    public static final String SSL_KEYSTORE_PASSWORD                = "ssl.keystore.password";
    public static final String SSL_KEYSTORE_TYPE                    = "ssl.keystore.type";
    public static final String SSL_KEY_PASSWORD_CONFIG              = "ssl.key.password";


    private static final Conf DEFAULT_CONF = Conf.of(
        HTTP_AUTHORIZATION_MANAGER_CLASS_CONFIG, SimpleAuthorizationManager.class.getName(),
        REST_AUTHENTICATION_ROLES_CONFIG, "*",
        REST_AUTHENTICATION_REALM_CONFIG, "AzkarraServer",
        SSL_KEYSTORE_TYPE, "PKCS12",
        SSL_TRUSTSTORE_TYPE, "PKCS12"
    );

    /**
     * Creates a new {@link SecurityConfig} instance.
     *
     * @param conf  the {@link Conf} instance, cannot be {@code null}.
     */
    public SecurityConfig(final Conf conf) {
        super(conf.withFallback(DEFAULT_CONF));
    }

    public boolean isHostnameVerificationIgnored() {
        return getOptionalBoolean(SSL_IGNORE_HOSTNAME_VERIFICATION).orElse(false);
    }

    public boolean isBasicAuthenticationSilent() {
        return getOptionalBoolean(REST_AUTHENTICATION_BASIC_SILENT_CONFIG).orElse(false);
    }

    public String getAuthenticationMechanism() {
        return getOptionalString(REST_AUTHENTICATION_MECHANISM_CONFIG).orElse(null);
    }

    public String getAuthenticationUsers() {
        return getOptionalString(REST_AUTHENTICATION_USERS_CONFIG).orElse("");
    }

    public AuthorizationManager getAuthorizationManager() {
        AuthorizationManager o = getClass(
            HTTP_AUTHORIZATION_MANAGER_CLASS_CONFIG,
            AuthorizationManager.class
        );
        Configurable.mayConfigure(o, this);
        return o;
    }

    public UsersIdentityManager getUserIdentityManager() {
        if (!hasPath(HTTP_AUTH_USER_IDENTITY_MANAGER_CLASS_CONFIG)) return null;

        UsersIdentityManager o = getClass(
            HTTP_AUTH_USER_IDENTITY_MANAGER_CLASS_CONFIG,
            UsersIdentityManager.class
        );
        Configurable.mayConfigure(o, this);
        return o;
    }

    public AzkarraPrincipalBuilder getAuthenticationPrincipalBuilder() {
        if (hasPath(HTTP_AUTH_PRINCIPAL_BUILDER_CLASS_CONFIG)) {
            AzkarraPrincipalBuilder o = getClass(
                HTTP_AUTH_PRINCIPAL_BUILDER_CLASS_CONFIG,
                AzkarraPrincipalBuilder.class
            );
            Configurable.mayConfigure(o, this);
            return o;
        }
        return null;
    }

    public String getAuthenticationRealm() {
        return getString(REST_AUTHENTICATION_REALM_CONFIG);
    }

    public String getAuthenticationRoles() {
        return getString(REST_AUTHENTICATION_ROLES_CONFIG);
    }

    public String getAuthenticationRestricted() {
        return getOptionalString(HTTP_RESTRICTED_ROLES_CONFIG).orElse("");
    }

    public boolean isHeadless() {
        return getOptionalBoolean(HTTP_HEADLESS_CONFIG).orElse(false);
    }

    public boolean isRestAuthenticationEnable() {
        return hasPath(REST_AUTHENTICATION_MECHANISM_CONFIG);
    }

    public boolean isSslEnable() {
        return getOptionalBoolean(SSL_ENABLE).orElse(false);
    }

    public String getKeystoreLocation() {
        return getString(SSL_KEYSTORE_LOCATION);
    }

    public char[] getKeystorePassword() {
        return getString(SSL_KEYSTORE_PASSWORD).toCharArray();
    }

    public String getKeystoreType() {
        return getString(SSL_KEYSTORE_TYPE);
    }

    public char[] getKeyPassword() {
        return getOptionalString(SSL_KEY_PASSWORD_CONFIG)
            .stream()
            .map(String::toCharArray)
            .findFirst()
            .orElse(getKeystorePassword());
    }

    public String getTrustStoreLocation() {
        return getOptionalString(SSL_TRUSTSTORE_LOCATION).orElse(null);
    }

    public char[] getTruststorePassword() {
        return getOptionalString(SSL_TRUSTSTORE_PASSWORD)
            .stream()
            .map(String::toCharArray)
            .findFirst()
            .orElse(null);
    }

    public String getTruststoreType() {
        return getString(SSL_TRUSTSTORE_TYPE);
    }
}

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
import io.streamthoughts.azkarra.api.config.ConfBuilder;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.http.security.auth.AzkarraPrincipalBuilder;
import io.streamthoughts.azkarra.http.security.auth.UsersIdentityManager;
import io.streamthoughts.azkarra.http.security.authorizer.AuthorizationManager;
import io.streamthoughts.azkarra.http.security.authorizer.SimpleAuthorizationManager;

import java.util.Objects;

public class SecurityConfig {

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


    private static final Conf DEFAULT_CONF = ConfBuilder.newConf()
        .with(HTTP_AUTHORIZATION_MANAGER_CLASS_CONFIG, SimpleAuthorizationManager.class.getName())
        .with(REST_AUTHENTICATION_ROLES_CONFIG, "*")
        .with(REST_AUTHENTICATION_REALM_CONFIG, "AzkarraServer")
        .with(SSL_KEYSTORE_TYPE, "PKCS12")
        .with(SSL_TRUSTSTORE_TYPE, "PKCS12")
        .build();

    private final Conf conf;

    /**
     * Creates a new {@link SecurityConfig} instance.
     *
     * @param conf  the {@link Conf} instance, cannot be {@code null}.
     */
    public SecurityConfig(final Conf conf) {
        Objects.requireNonNull(conf, "conf cannot be null");
        this.conf = conf.withFallback(DEFAULT_CONF);
    }

    public boolean isHostnameVerificationIgnored() {
        return conf.getOptionalBoolean(SSL_IGNORE_HOSTNAME_VERIFICATION).orElse(false);
    }

    public boolean isBasicAuthenticationSilent() {
        return conf.getOptionalBoolean(REST_AUTHENTICATION_BASIC_SILENT_CONFIG).orElse(false);
    }

    public String getAuthenticationMechanism() {
        return conf.getOptionalString(REST_AUTHENTICATION_MECHANISM_CONFIG).orElse(null);
    }

    public String getAuthenticationUsers() {
        return conf.getOptionalString(REST_AUTHENTICATION_USERS_CONFIG).orElse("");
    }

    public AuthorizationManager getAuthorizationManager() {
        AuthorizationManager o = conf.getClass(
            HTTP_AUTHORIZATION_MANAGER_CLASS_CONFIG,
            AuthorizationManager.class
        );
        Configurable.mayConfigure(o, conf);
        return o;
    }

    public UsersIdentityManager getUserIdentityManager() {
        if (!conf.hasPath(HTTP_AUTH_USER_IDENTITY_MANAGER_CLASS_CONFIG)) return null;

        UsersIdentityManager o = conf.getClass(
            HTTP_AUTH_USER_IDENTITY_MANAGER_CLASS_CONFIG,
            UsersIdentityManager.class
        );
        Configurable.mayConfigure(o, conf);
        return o;
    }

    public AzkarraPrincipalBuilder getAuthenticationPrincipalBuilder() {
        if (conf.hasPath(HTTP_AUTH_PRINCIPAL_BUILDER_CLASS_CONFIG)) {
            AzkarraPrincipalBuilder o = conf.getClass(
                HTTP_AUTH_PRINCIPAL_BUILDER_CLASS_CONFIG,
                AzkarraPrincipalBuilder.class
            );
            Configurable.mayConfigure(o, conf);
            return o;
        }
        return null;
    }

    public String getAuthenticationRealm() {
        return conf.getString(REST_AUTHENTICATION_REALM_CONFIG);
    }

    public String getAuthenticationRoles() {
        return conf.getString(REST_AUTHENTICATION_ROLES_CONFIG);
    }

    public String getAuthenticationRestricted() {
        return conf.getOptionalString(HTTP_RESTRICTED_ROLES_CONFIG).orElse("");
    }

    public boolean isHeadless() {
        return conf.getOptionalBoolean(HTTP_HEADLESS_CONFIG).orElse(false);
    }

    public boolean isRestAuthenticationEnable() {
        return conf.hasPath(REST_AUTHENTICATION_MECHANISM_CONFIG);
    }

    public boolean isSslEnable() {
        return conf.getOptionalBoolean(SSL_ENABLE).orElse(false);
    }

    public String getKeystoreLocation() {
        return conf.getString(SSL_KEYSTORE_LOCATION);
    }

    public char[] getKeystorePassword() {
        return conf.getString(SSL_KEYSTORE_PASSWORD).toCharArray();
    }

    public String getKeystoreType() {
        return conf.getString(SSL_KEYSTORE_TYPE);
    }

    public char[] getKeyPassword() {
        return conf.getOptionalString(SSL_KEY_PASSWORD_CONFIG)
            .stream()
            .map(String::toCharArray)
            .findFirst()
            .orElse(getKeystorePassword());
    }

    public String getTrustStoreLocation() {
        return conf.getOptionalString(SSL_TRUSTSTORE_LOCATION).orElse(null);
    }

    public char[] getTruststorePassword() {
        return conf.getOptionalString(SSL_TRUSTSTORE_PASSWORD)
            .stream()
            .map(String::toCharArray)
            .findFirst()
            .orElse(null);
    }

    public String getTruststoreType() {
        return conf.getString(SSL_TRUSTSTORE_TYPE);
    }
}

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

/**
 * The server configuration.
 */
public class ServerConfig extends SecurityConfig {

    public static final String HTTP_SERVER_PORT_CONFIG = "port";
    public static final int HTTP_SERVER_PORT_DEFAULT = 8080;

    public static final String HTTP_SERVER_LISTENER_CONFIG  = "listener";
    private static final String HTTP_SERVER_LISTENER_DEFAULT = "localhost";

    public static final String HTTP_SERVER_UI_ENABLE_CONFIG = "enable.ui";
    public static final String HTTP_SERVER_REST_EXTENSIONS_ENABLE = "rest.extensions.enable";



    public static ServerConfigBuilder newBuilder() {
        return new ServerConfigBuilder();
    }

    public static ServerConfig of(final Conf conf) {
        if (conf instanceof ServerConfig)
            return (ServerConfig)conf;
        return new ServerConfig(conf);
    }

    /**
     * Creates a new {@link SecurityConfig} instance.
     *
     * @param conf the {@link Conf} instance, cannot be {@code null}.
     */
    public ServerConfig(final Conf conf) {
        super(conf);
    }

    public int getPort()  {
        return getOptionalInt(HTTP_SERVER_PORT_CONFIG).orElse(HTTP_SERVER_PORT_DEFAULT);
    }

    public String getListener()  {
        return getOptionalString(HTTP_SERVER_LISTENER_CONFIG).orElse(HTTP_SERVER_LISTENER_DEFAULT);
    }

    public boolean isUIEnable()  {
        // by default Web UI should always be enable.
        return getOptionalBoolean(HTTP_SERVER_UI_ENABLE_CONFIG).orElse(true);
    }

    public boolean isRestExtensionEnable() {
        // by default REST extensions support should be disable.
        return getOptionalBoolean(HTTP_SERVER_REST_EXTENSIONS_ENABLE).orElse(false);
    }

}

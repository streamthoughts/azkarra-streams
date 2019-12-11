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
package io.streamthoughts.azkarra.api.server;

import java.util.Objects;

public class ServerInfo {

    private final String host;
    private final int port;
    private final boolean sslEnable;

    /**
     * Creates a new {@link ServerInfo} instance.
     *
     * @param host  the server host.
     * @param port  the server port.
     */
    public ServerInfo(final String host, final int port) {
        this(host, port, false);
    }

    /**
     * Creates a new {@link ServerInfo} instance.
     *
     * @param host  the server host.
     * @param port  the server port.
     */
    public ServerInfo(final String host, final int port, boolean sslEnable) {
        this.host = host;
        this.port = port;
        this.sslEnable = sslEnable;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerInfo)) return false;
        ServerInfo that = (ServerInfo) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (sslEnable ? "https" : "http") + "://" + host + ":" + port;
    }
}

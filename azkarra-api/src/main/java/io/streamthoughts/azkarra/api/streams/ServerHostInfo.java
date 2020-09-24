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
package io.streamthoughts.azkarra.api.streams;

import java.util.Objects;

public class ServerHostInfo {

    private final String id;
    private final String host;
    private final int port;
    private final boolean isLocal;

    /**
     * Creates a new {@link ServerHostInfo} instance.
     *
     * @param id        the application id.
     * @param host      the application server host.
     * @param port      the application server port.
     * @param isLocal   flag to indicate if server is local.
     */
    public ServerHostInfo(final String id,
                   final String host,
                   final int port,
                   final boolean isLocal) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.isLocal = isLocal;
    }

    /**
     * Gets the stream application id.
     *
     * @return the string id.
     */
    public String id() {
        return id;
    }

    /**
     * Gets the stream application host.
     *
     * @return the string host.
     */
    public String host() {
        return host;
    }

    /**
     * Gets the stream application port.
     *
     * @return the string port.
     */
    public int port() {
        return port;
    }

    /**
     * Checks whether this instance is local.
     *
     * @return the {@code true} if local.
     */
    public boolean isLocal() {
        return isLocal;
    }

    public String hostAndPort() {
        return host + ":" + port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerHostInfo)) return false;
        ServerHostInfo that = (ServerHostInfo) o;
        return port == that.port &&
                isLocal == that.isLocal &&
                Objects.equals(id, that.id) &&
                Objects.equals(host, that.host);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, host, port, isLocal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ServerHostInfo{" +
                "id='" + id + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", isLocal=" + isLocal +
                '}';
    }
}

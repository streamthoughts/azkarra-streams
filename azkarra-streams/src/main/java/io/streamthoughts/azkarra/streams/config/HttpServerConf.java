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
package io.streamthoughts.azkarra.streams.config;

import io.streamthoughts.azkarra.api.config.Conf;

import java.util.HashMap;
import java.util.Map;

public class HttpServerConf {

    public static HttpServerConf with(final Conf conf) {
        return new HttpServerConf(null, null, new HashMap<>(), conf);
    }

    public static HttpServerConf with(final String listener, final int port) {
        return new HttpServerConf(listener, port, new HashMap<>(), Conf.empty());
    }

    protected final String listener;
    protected final Integer port;
    protected final Conf originals;
    protected final Map<String, Object> overrides;

    /**
     * Creates a new {@link HttpServerConf} instance.
     *
     * @param conf  a {@link HttpServerConf} instance.
     */
    protected HttpServerConf(final HttpServerConf conf) {
        this(conf.listener, conf.port, conf.overrides, conf.originals);
    }

    /**
     * Creates a new {@link HttpServerConf} instance.
     *
     * @param listener  the server listener.
     * @param port      the server port.
     */
    protected HttpServerConf(final String listener,
                           final Integer port,
                           final Map<String, Object> overrides,
                           final Conf originals) {
        this.listener = listener;
        this.port = port;
        this.overrides = overrides;
        this.originals = originals;
    }

    /**
     * Sets the http server port.
     *
     * @param port  the http port.
     * @return      a new {@link HttpServerConf} instance.
     */
    public HttpServerConf withPort(final int port) {
        return new HttpServerConf(listener, port,  overrides, originals);
    }

    /**
     * Sets the http server listener.
     *
     * @param listener  the http listener.
     * @return          a new {@link HttpServerConf} instance.
     */
    public HttpServerConf withListener(final String listener) {
        return new HttpServerConf(listener, port, overrides, originals);
    }

    public HttpServerConf withConfig(final String key, final Object value) {
        Map<String, Object> overrides = new HashMap<>(this.overrides);
        overrides.put(key, value);
        return new HttpServerConf(listener, port, overrides, originals);
    }
}

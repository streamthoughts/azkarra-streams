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
package io.streamthoughts.azkarra.streams.config.internal;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.streams.config.HttpServerConf;

import java.util.HashMap;
import java.util.Map;

public class InternalHttpServerConf extends HttpServerConf {

    private static final String HTTP_PORT_CONFIG                    = "port";
    private static final String HTTP_LISTENER_LISTER_CONFIG         = "listener";

    /**
     * Creates a new {@link InternalHttpServerConf} instance.
     *
     * @param original  the {@link HttpServerConf} conf.
     */
    public InternalHttpServerConf(final HttpServerConf original) {
        super(original);
    }

    public InternalHttpServerConf(final String listener, final int port) {
        super(listener, port, new HashMap<>(), Conf.empty());
    }
    
    public Conf getConf() {
        Map<String, Object> configs = new HashMap<>();
        if (listener != null) {
            configs.put(HTTP_LISTENER_LISTER_CONFIG, listener);
        }
        if (port != null) {
            configs.put(HTTP_PORT_CONFIG, port);
        }
        configs.putAll(overrides);
        return Conf.with(configs).withFallback(originals);
    }
}

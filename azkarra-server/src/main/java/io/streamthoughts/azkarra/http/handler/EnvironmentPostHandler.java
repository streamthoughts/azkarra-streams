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
package io.streamthoughts.azkarra.http.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.error.BadRequestException;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

import static io.streamthoughts.azkarra.api.util.Utils.isNullOrEmpty;


public class EnvironmentPostHandler extends AbstractStreamHttpHandler {

    /**
     * Creates a new {@link EnvironmentPostHandler} instance.
     *
     * @param service   the {@link AzkarraStreamsService} instance.
     */
    public EnvironmentPostHandler(final AzkarraStreamsService service) {
        super(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) {
        final EnvironmentPayload env = ExchangeHelper.readJsonRequest(exchange, EnvironmentPayload.class);
        if (isNullOrEmpty(env.name)) {
            throw new BadRequestException("Invalid JSON field, 'name' cannot be null.");
        }
        service.addNewEnvironment(env.name, Conf.of(env.config));
    }

    public static final class EnvironmentPayload {

        public final String name;
        public final Map<String, Object> config;

        @JsonCreator
        public EnvironmentPayload(@JsonProperty("name") final String name,
                                  @JsonProperty("config") final Map<String, Object> config) {
            this.name = name;
            this.config = config;
        }
    }
}

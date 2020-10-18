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
package io.streamthoughts.azkarra.http.routes;

import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.util.Version;
import io.streamthoughts.azkarra.http.APIVersions;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.handler.AbstractStreamHttpHandler;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;

import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class defines all routes for API '/topologies'.
 */
public class ApiTopologyRoutes implements RoutingHandlerProvider {

    /**
     * Creates a new {@link ApiTopologyRoutes} instance.
     */
    public ApiTopologyRoutes() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingHandler handler(final AzkarraStreamsService service) {

        return Handlers.routing()
            .get(APIVersions.PATH_V1 + "/topologies",
                handlerFor(service, exchange -> {
                    ExchangeHelper.sendJsonResponse(exchange, service.getAllTopologies());
                })
            )
            .get(APIVersions.PATH_V1 + "/topologies/{type}/versions",
                handlerFor(service, exchange -> {
                    var type = ExchangeHelper.getQueryParam(exchange, "type");
                    var versions = service.getTopologyVersionsByAlias(type)
                        .stream()
                        .map(Version::toString)
                        .collect(Collectors.toSet());
                    ExchangeHelper.sendJsonResponse(exchange, versions);
                })
            )
            .get(APIVersions.PATH_V1 + "/topologies/{type}/versions/{version}",
                handlerFor(service, exchange -> {
                    var type = ExchangeHelper.getQueryParam(exchange, "type");
                    var version = ExchangeHelper.getQueryParam(exchange, "version");
                    ExchangeHelper.sendJsonResponse(exchange,
                        service.getTopologyByAliasAndVersion(type, version)
                    );
                })
            );
    }

    private HttpHandler handlerFor(final AzkarraStreamsService service,
                                   final Consumer<HttpServerExchange> handler) {
        return new AbstractStreamHttpHandler(service) {
            @Override
            public void handleRequest(HttpServerExchange exchange) {
                handler.accept(exchange);
            }
        };
    }


}

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
package io.streamthoughts.azkarra.http.routes;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.health.Health;
import io.streamthoughts.azkarra.http.health.HealthAggregator;
import io.streamthoughts.azkarra.http.health.HealthIndicator;
import io.streamthoughts.azkarra.http.health.internal.StreamsHealthIndicator;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ApiHealthRoutes implements HttpHandler, RoutingHandlerProvider, AzkarraContextAware {

    private AzkarraContext context;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAzkarraContext(final AzkarraContext context) {
        this.context = context;
        this.context.addComponent(StreamsHealthIndicator.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingHandler handler(final AzkarraStreamsService service) {
        return Handlers.routing().get("/health", this) ;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) {
        Collection<HealthIndicator> indicators = context.getAllComponentForType(HealthIndicator.class);
        HealthAggregator aggregator = new HealthAggregator();
        List<Health> healths = indicators
            .stream()
            .map(HealthIndicator::getHealth)
            .collect(Collectors.toList());
        ExchangeHelper.sendJsonResponse(exchange, aggregator.aggregate(healths));
    }
}

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
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.errors.InvalidConfException;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiInfoRoutes implements HttpHandler, RoutingHandlerProvider, Configurable {

    private Map<String, Object> info = Collections.emptyMap();

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        if (configuration.hasPath("info")) {
            Conf infoConf = configuration.getSubConf("info");
            info = explode(infoConf.getConfAsMap());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingHandler handler(final AzkarraStreamsService service) {
        return Handlers.routing().get("/info", this) ;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) {
        ExchangeHelper.sendJsonResponse(exchange, info);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> explode(final Map<String, ?> map) {
        final Stream<Tuple<String, ?>> tupleStream = map.entrySet()
            .stream()
            .map(Tuple::of)
            .map(tuple -> {
                if (tuple.left().contains(".")) {
                    final String[] split = tuple.left().split("\\.", 2);
                    final Map<String, Object> nested = explode(Collections.singletonMap(split[1], tuple.right()));
                    return new Tuple<>(split[0], nested);
                }
                else if (tuple.right() instanceof Map) {
                    return tuple.mapValue(m -> explode((Map)m));
                }
                return tuple;
            });
        return tupleStream.collect(Collectors.toMap(Tuple::left, Tuple::right, ApiInfoRoutes::merge));
    }

    static Object merge(final Object o1, final Object o2) {
        try {
            final Set<? extends Map.Entry<String, ?>> e1 = ((Map<String, Object>) o1).entrySet();
            final Set<? extends Map.Entry<String, ?>> e2 = ((Map<String, Object>) o2).entrySet();
            return Stream.of(e1, e2)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, ApiInfoRoutes::merge));

        } catch (ClassCastException e) {
            throw new InvalidConfException(
                String.format(
                    "Cannot merge two parameters with different type : %s<>%s",
                    o1.getClass().getName(),
                    o2.getClass().getName()
                )
            );
        }
    }
}

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

import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.undertow.Handlers;
import io.undertow.predicate.Predicates;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Methods;

public class WebUIHttpRoutes implements RoutingHandlerProvider {

    private static final String UI_STATIC_CLASSPATH = "io/streamthoughts/azkarra/ui/";

    private static final ClassPathResourceManager resourceManager = new ClassPathResourceManager(
        ClassUtils.getClassLoader(),
        UI_STATIC_CLASSPATH
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingHandler handler(final AzkarraStreamsService service) {
        final ResourceHandler resourceHandler = Handlers.resource(resourceManager);

        final PredicateHandler predicateHandler = new PredicateHandler(
            Predicates.suffixes(".css", ".js", ".html", ".png", ".svg", ".map"),
            resourceHandler,
            new ApiVersionRoutes());

        RoutingHandler handler = Handlers.routing();
        return handler
            .add(Methods.HEAD, "/static/*", predicateHandler)
            .add(Methods.POST, "/static/*", predicateHandler)
            .add(Methods.GET, "/static/*", predicateHandler)
            .get("/ui", new ResourceHandler(
                    (exchange, path) -> resourceManager.getResource("index.html"))
            );
    }
}

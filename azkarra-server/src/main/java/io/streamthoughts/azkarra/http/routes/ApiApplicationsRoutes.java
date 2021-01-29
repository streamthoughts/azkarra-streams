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

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.AzkarraContextAware;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.query.InteractiveQueryService;
import io.streamthoughts.azkarra.http.APIVersions;
import io.streamthoughts.azkarra.http.handler.ApplicationQueryStoreHandler;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;

import static io.streamthoughts.azkarra.http.ExchangeHelper.getQueryParam;
import static io.streamthoughts.azkarra.http.ExchangeHelper.sendJsonResponse;
import static io.streamthoughts.azkarra.http.utils.Constants.HTTP_QUERY_PARAM_ID;

/**
 * This class defines all routes for API '/applications'.
 */
public class ApiApplicationsRoutes implements RoutingHandlerProvider, AzkarraContextAware {

    private static final String APPLICATIONS_PREFIX_PATH = APIVersions.PATH_V1 + "/applications";

    private AzkarraContext context;

    /**
     * Creates a new {@link ApiApplicationsRoutes} instance.
     */
    public ApiApplicationsRoutes() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingHandler handler(final AzkarraStreamsService service) {
        final InteractiveQueryService queryService = context.getComponent(InteractiveQueryService.class);
        return Handlers.routing()
            .get(template(""), exchange ->
                sendJsonResponse(exchange, service.listAllKafkaStreamsApplicationIds()))

            .get(template("/{id}"), exchange ->
                sendJsonResponse(exchange, service.getStreamsApplicationById(getQueryParam(exchange, HTTP_QUERY_PARAM_ID))))

            .delete(template("/{id}"), exchange ->
                service.terminateStreamsApplication(getQueryParam(exchange, HTTP_QUERY_PARAM_ID)))

            .post(template("/{id}/stores/{storeName}"),
                    new BlockingHandler(new ApplicationQueryStoreHandler(queryService, false)))

            .post(template("/{id}/stores/{storeName}/records"),
                    new BlockingHandler(new ApplicationQueryStoreHandler(queryService, true)));
    }

    private static String template(final String path) {
        return APPLICATIONS_PREFIX_PATH + path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAzkarraContext(final AzkarraContext context) {
        this.context = context;
    }
}
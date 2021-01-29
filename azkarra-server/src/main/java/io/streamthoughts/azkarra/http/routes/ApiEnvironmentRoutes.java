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
import io.streamthoughts.azkarra.http.APIVersions;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.data.EnvironmentRequestBody;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;

import static io.streamthoughts.azkarra.http.ExchangeHelper.getQueryParam;
import static io.streamthoughts.azkarra.http.utils.Constants.HTTP_QUERY_PARAM_ID;

public class ApiEnvironmentRoutes implements RoutingHandlerProvider {

    public static final String PATH_ENVIRONMENTS = APIVersions.PATH_V1 + "/environments";
    public static final String PATH_ENVIRONMENT_TYPES = APIVersions.PATH_V1 + "/environment-types";

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingHandler handler(final AzkarraStreamsService service) {
        return Handlers.routing()
            .get(template(""), exchange ->
                ExchangeHelper.sendJsonResponse(exchange, service.describeAllEnvironments()))
            .get(template("/{id}"), exchange ->
                ExchangeHelper.sendJsonResponse(exchange, service.describeEnvironmentByName(getQueryParam(exchange, HTTP_QUERY_PARAM_ID))))
            .get(PATH_ENVIRONMENT_TYPES, exchange ->
                ExchangeHelper.sendJsonResponse(exchange, service.getSupportedEnvironmentTypes()))
            .post(template(""), new BlockingHandler(exchange -> {
                var request = ExchangeHelper.readJsonRequest(exchange, EnvironmentRequestBody.class);
                service.addNewEnvironment(request.name, request.type, Conf.of(request.config));
            }));
    }


    private static String template(final String path) {
        return PATH_ENVIRONMENTS + path;
    }
}

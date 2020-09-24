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
import io.streamthoughts.azkarra.http.APIVersions;
import io.streamthoughts.azkarra.http.handler.ApplicationGetInstancesHandler;
import io.streamthoughts.azkarra.http.handler.ApplicationGetTopologyHandler;
import io.streamthoughts.azkarra.http.handler.ApplicationQueryStoreHandler;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;

/**
 * This class defines all routes for API '/applications'.
 */
public class ApiApplicationsRoutes implements RoutingHandlerProvider {

    private static final String APPLICATIONS_PREFIX_PATH = APIVersions.PATH_V1 + "/applications/";

    /**
     * Creates a new {@link ApiApplicationsRoutes} instance.
     */
    public ApiApplicationsRoutes() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingHandler handler(final AzkarraStreamsService service) {

        return Handlers.routing()
            .get(templatePath("{id}"),
                new ApplicationGetInstancesHandler(service))

            .get(templatePath("{id}/topology"),
                new ApplicationGetTopologyHandler(service))

            .post(templatePath("{id}/stores/{storeName}"),
                new BlockingHandler(new ApplicationQueryStoreHandler(service, false)))

            .post(templatePath("{id}/stores/{storeName}/records"),
                new BlockingHandler(new ApplicationQueryStoreHandler(service, true)));
    }

    private String templatePath(final String assignments) {
        return APPLICATIONS_PREFIX_PATH + assignments;
    }

}

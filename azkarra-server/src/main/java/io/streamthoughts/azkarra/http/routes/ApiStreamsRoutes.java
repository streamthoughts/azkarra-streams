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
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.http.handler.StreamsDeleteHandler;
import io.streamthoughts.azkarra.http.handler.StreamsGetConfigHandler;
import io.streamthoughts.azkarra.http.handler.StreamsGetDetailsHandler;
import io.streamthoughts.azkarra.http.handler.StreamsGetListHandler;
import io.streamthoughts.azkarra.http.handler.StreamsGetMetricsHandler;
import io.streamthoughts.azkarra.http.handler.StreamsGetOffsetsHandler;
import io.streamthoughts.azkarra.http.handler.StreamsGetStatusHandler;
import io.streamthoughts.azkarra.http.handler.StreamsPostHandler;
import io.streamthoughts.azkarra.http.handler.StreamsRestartHandler;
import io.streamthoughts.azkarra.http.handler.StreamsStopHandler;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.streamthoughts.azkarra.http.sse.EventStreamConnectionCallback;
import io.undertow.Handlers;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;

/**
 * This class defines all routes for API '/streams'.
 */
public class ApiStreamsRoutes implements RoutingHandlerProvider {

    private static final String PATH_STREAMS
            = "/streams";
    private static final String PATH_STREAMS_ID
            = "/streams/{id}";
    private static final String PATH_STREAMS_STATUS
            = "/streams/{id}/status";
    private static final String PATH_STREAMS_OFFSETS
            = "/streams/{id}/offsets";
    private static final String PATH_STREAMS_CONFIG
            = "/streams/{id}/config";
    private static final String PATH_STREAMS_METRICS
            = "/streams/{id}/metrics";
    private static final String PATH_STREAMS_METRICS_GROUP
            = "/streams/{id}/metrics/group/{group}";
    private static final String PATH_STREAMS_METRICS_GROUP_METRIC
            = "/streams/{id}/metrics/group/{group}/metric/{metric}";
    private static final String PATH_STREAMS_METRICS_GROUP_METRIC_VALUE
            = "/streams/{id}/metrics/group/{group}/metric/{metric}/value";
    private static final String PATH_STREAMS_RESTART
            = "/streams/{id}/restart";
    private static final String PATH_STREAMS_STOP
            = "/streams/{id}/stop";
    private static final String PATH_STREAMS_SSE
            = "/streams/{id}/subscribe/{event}";

    /**
     * Creates a new {@link ApiStreamsRoutes} instance.
     */
    public ApiStreamsRoutes() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingHandler handler(final AzkarraStreamsService service) {

        final BlockingHandler metricsHandler = new BlockingHandler(new StreamsGetMetricsHandler(service));

        return Handlers.routing()
            .get(APIVersions.PATH_V1 + PATH_STREAMS,
                new StreamsGetListHandler(service))
            .post(APIVersions.PATH_V1 + PATH_STREAMS,
                new BlockingHandler(new StreamsPostHandler(service)))
            .get(APIVersions.PATH_V1 + PATH_STREAMS_ID,
                new StreamsGetDetailsHandler(service))
            .get(APIVersions.PATH_V1 + PATH_STREAMS_STATUS,
                new StreamsGetStatusHandler(service))
            .get(APIVersions.PATH_V1 + PATH_STREAMS_CONFIG,
                new StreamsGetConfigHandler(service))
            .post(APIVersions.PATH_V1 + PATH_STREAMS_RESTART,
                new StreamsRestartHandler(service))
            .post(APIVersions.PATH_V1 + PATH_STREAMS_STOP,
                new BlockingHandler(new StreamsStopHandler(service)))
            .delete(APIVersions.PATH_V1 + PATH_STREAMS_ID,
                new StreamsDeleteHandler(service))
            .get(APIVersions.PATH_V1 + PATH_STREAMS_OFFSETS,
                new StreamsGetOffsetsHandler(service))
            .get(APIVersions.PATH_V1 + PATH_STREAMS_METRICS, metricsHandler)
            .get(APIVersions.PATH_V1 + PATH_STREAMS_METRICS_GROUP, metricsHandler)
            .get(APIVersions.PATH_V1 + PATH_STREAMS_METRICS_GROUP_METRIC, metricsHandler)
            .get(APIVersions.PATH_V1 + PATH_STREAMS_METRICS_GROUP_METRIC_VALUE, metricsHandler)
            .get(APIVersions.PATH_V1 + PATH_STREAMS_SSE,
                Handlers.serverSentEvents(new EventStreamConnectionCallback(service, ExchangeHelper.JSON))
            );
    }
}

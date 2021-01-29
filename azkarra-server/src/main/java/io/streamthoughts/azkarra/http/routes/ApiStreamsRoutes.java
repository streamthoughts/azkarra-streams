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
import io.streamthoughts.azkarra.http.data.StreamsInstanceStatus;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.topology.TopologyMetadata;
import io.streamthoughts.azkarra.api.util.Utils;
import io.streamthoughts.azkarra.http.APIVersions;
import io.streamthoughts.azkarra.http.data.StreamsInstanceInfo;
import io.streamthoughts.azkarra.http.handler.StreamsGetMetricsHandler;
import io.streamthoughts.azkarra.http.handler.StreamsPostHandler;
import io.streamthoughts.azkarra.http.handler.StreamsStopHandler;
import io.streamthoughts.azkarra.http.spi.RoutingHandlerProvider;
import io.streamthoughts.azkarra.http.sse.EventStreamConnectionCallback;
import io.undertow.Handlers;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;

import static io.streamthoughts.azkarra.http.ExchangeHelper.JSON;
import static io.streamthoughts.azkarra.http.ExchangeHelper.getQueryParam;
import static io.streamthoughts.azkarra.http.ExchangeHelper.sendJsonResponse;
import static io.streamthoughts.azkarra.http.utils.Constants.HTTP_QUERY_PARAM_ID;

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
    private static final String PATH_STREAMS_METADATA
            = "/streams/{id}/metadata";
    private static final String PATH_STREAMS_METRICS
            = "/streams/{id}/metrics";
    private static final String PATH_STREAMS_STATES
            = "/streams/{id}/state-offsets";
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
    private static final String PATH_STREAMS_EVENT_STREAMS
            = "/streams/{id}/subscriptions/";
    private static final String PATH_STREAMS_EVENT_STREAMS_SSE
            = "/streams/{id}/subscribe/{event}";
    private static final String PATH_STREAMS_TOPOLOGY
            = "/streams/{id}/topology";

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingHandler handler(final AzkarraStreamsService service) {

        final BlockingHandler metricsHandler = new BlockingHandler(new StreamsGetMetricsHandler(service));

        return Handlers.routing()
            .get(APIVersions.PATH_V1 + PATH_STREAMS, exchange ->
                sendJsonResponse(exchange, service.listAllKafkaStreamsContainerIds()))

            .post(APIVersions.PATH_V1 + PATH_STREAMS,
                new BlockingHandler(new StreamsPostHandler(service))
            )
            .get(APIVersions.PATH_V1 + PATH_STREAMS_ID, exchange ->
                sendJsonResponse(
                    exchange,
                    newStreamsInfo(service.getStreamsContainerById(getQueryParam(exchange, HTTP_QUERY_PARAM_ID)))
                )
            )
            .get(APIVersions.PATH_V1 + PATH_STREAMS_STATUS, exchange ->
                sendJsonResponse(
                    exchange,
                    newStreamsStatus(service.getStreamsContainerById(getQueryParam(exchange, HTTP_QUERY_PARAM_ID)))
                )
            )
            .get(APIVersions.PATH_V1 + PATH_STREAMS_CONFIG, exchange ->
                sendJsonResponse(
                    exchange,
                    service.getStreamsContainerById(getQueryParam(exchange, HTTP_QUERY_PARAM_ID))
                            .streamsConfig()
                )
            )
            .get(APIVersions.PATH_V1 + PATH_STREAMS_OFFSETS, exchange ->
                sendJsonResponse(
                    exchange,
                    service.getStreamsContainerById(getQueryParam(exchange, HTTP_QUERY_PARAM_ID))
                            .offsets()
                )
            )
            .get(APIVersions.PATH_V1 + PATH_STREAMS_METADATA, exchange ->
                sendJsonResponse(
                    exchange,
                    service.getStreamsContainerById(getQueryParam(exchange, HTTP_QUERY_PARAM_ID))
                            .describe()
                            .metadata()
                )
            )
            .get(APIVersions.PATH_V1 + PATH_STREAMS_STATES, exchange ->
                sendJsonResponse(
                    exchange,
                    service.getStreamsContainerById(getQueryParam(exchange, HTTP_QUERY_PARAM_ID))
                        .allLocalStorePartitionLags()
                )
            )
            .get(APIVersions.PATH_V1 + PATH_STREAMS_TOPOLOGY, exchange ->
                sendJsonResponse(
                    exchange,
                    service.getStreamsContainerById(getQueryParam(exchange, HTTP_QUERY_PARAM_ID)).topologyGraph())
            )
            .post(APIVersions.PATH_V1 + PATH_STREAMS_RESTART, exchange ->
                service.restartStreamsContainer(getQueryParam(exchange, HTTP_QUERY_PARAM_ID)))

            .post(APIVersions.PATH_V1 + PATH_STREAMS_STOP,
                new BlockingHandler(new StreamsStopHandler(service)))

            .delete(APIVersions.PATH_V1 + PATH_STREAMS_ID, exchange ->
                service.terminateStreamsContainer(getQueryParam(exchange, HTTP_QUERY_PARAM_ID)))

            .get(APIVersions.PATH_V1 + PATH_STREAMS_METRICS, metricsHandler)
            .get(APIVersions.PATH_V1 + PATH_STREAMS_METRICS_GROUP, metricsHandler)
            .get(APIVersions.PATH_V1 + PATH_STREAMS_METRICS_GROUP_METRIC, metricsHandler)
            .get(APIVersions.PATH_V1 + PATH_STREAMS_METRICS_GROUP_METRIC_VALUE, metricsHandler)
            .get(APIVersions.PATH_V1 + PATH_STREAMS_EVENT_STREAMS_SSE,
                Handlers.serverSentEvents(new EventStreamConnectionCallback(service, JSON))
            )
            .get(APIVersions.PATH_V1 + PATH_STREAMS_EVENT_STREAMS, exchange ->
                sendJsonResponse(
                    exchange,
                    service.getStreamsContainerById(getQueryParam(exchange, HTTP_QUERY_PARAM_ID))
                          .listRegisteredEventStreamTypes()
                )
            );
    }

    private StreamsInstanceInfo newStreamsInfo(final KafkaStreamsContainer container) {
        TopologyMetadata metadata = container.topologyMetadata();
        return StreamsInstanceInfo.newBuilder()
            .setId(container.applicationId())
            .setSince(container.startedSince())
            .setName(metadata.name())
            .setVersion(metadata.version())
            .setDescription(metadata.description())
            .setState(container.state().value().name(), container.state().timestamp())
            .setException(container.exception().map(Utils::formatStackTrace).orElse(null))
            .setEndpoint(container.endpoint().orElse(null))
            .build();
    }

    private StreamsInstanceStatus newStreamsStatus(final KafkaStreamsContainer container) {
        return new StreamsInstanceStatus(
            container.applicationId(),
            container.state().value().name(),
            container.threadMetadata());
    }
}

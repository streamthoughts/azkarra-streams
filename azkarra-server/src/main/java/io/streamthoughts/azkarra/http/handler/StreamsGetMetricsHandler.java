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
package io.streamthoughts.azkarra.http.handler;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.model.Metric;
import io.streamthoughts.azkarra.api.model.MetricGroup;
import io.streamthoughts.azkarra.api.model.predicate.GroupMetricFilter;
import io.streamthoughts.azkarra.api.model.predicate.NameMetricFilter;
import io.streamthoughts.azkarra.api.model.predicate.NonNullMetricFilter;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.http.ExchangeHelper;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.http.error.MetricNotFoundException;
import io.streamthoughts.azkarra.http.prometheus.KafkaStreamsMetricsCollector;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class StreamsGetMetricsHandler extends AbstractStreamHttpHandler implements WithApplication {

    /**
     * Creates a new {@link StreamsGetMetricsHandler} instance.
     *
     * @param service   the {@link AzkarraStreamsService} instance.
     */
    public StreamsGetMetricsHandler(final AzkarraStreamsService service) {
        super(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange, final String applicationId) {
        final Optional<String> empty = ExchangeHelper.getOptionalQueryParam(exchange, "filter_empty");
        final Optional<String> group = ExchangeHelper.getOptionalQueryParam(exchange, "group");
        final Optional<String> name  = ExchangeHelper.getOptionalQueryParam(exchange, "metric");
        final Optional<String> format  = ExchangeHelper.getOptionalQueryParam(exchange, "format");

        Optional<Predicate<Tuple<String, Metric>>> all = Optional.of(t -> true);

        Predicate<Tuple<String, Metric>> filter = all
            .map(predicate -> empty.map(f -> new NonNullMetricFilter()).map(predicate::and).orElse(predicate))
            .map(predicate -> group.map(GroupMetricFilter::new).map(predicate::and).orElse(predicate))
            .map(predicate -> name.map(NameMetricFilter::new).map(predicate::and).orElse(predicate))
            .get();

        if (format.isPresent() && format.get().equals("prometheus")) {

            CollectorRegistry registry = new CollectorRegistry();
            new KafkaStreamsMetricsCollector(service, filter, applicationId).register(registry);

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);

            OutputStream outputStream = exchange.getOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                TextFormat.write004(writer, registry.filteredMetricFamilySamples(Collections.emptySet()));
            } catch (final IOException e) {
                throw new AzkarraException("Unexpected error happens while writing metrics", e);
            }
        } else {
            Set<MetricGroup> groupSet = service.getStreamsMetricsById(applicationId, filter);

            final Optional<Metric> metric = groupSet.stream()
                    .flatMap(g -> g.metrics().stream())
                    .findFirst();

            if (metric.isEmpty() && name.isPresent()) {
                throw new MetricNotFoundException("{group=\"" + group.get() + "\", metric=" + name.get() + "}");
            }

            if (metric.isEmpty() && group.isPresent()) {
                throw new MetricNotFoundException("{group=\"" + group.get() + "\"}");
            }

            boolean extractValue = exchange.getRelativePath().endsWith("/value");
            if (name.isPresent() && extractValue) {
                ExchangeHelper.sendJsonResponse(exchange, metric.get().value());
            } else {
                ExchangeHelper.sendJsonResponse(exchange, groupSet);
            }
        }
    }
}
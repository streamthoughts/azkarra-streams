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
package io.streamthoughts.azkarra.http.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.errors.NotFoundException;
import io.streamthoughts.azkarra.api.model.Metric;
import io.streamthoughts.azkarra.api.model.MetricGroup;
import io.streamthoughts.azkarra.api.monad.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Simple {@link Collector} implementation for collecting
 * metrics from {@link org.apache.kafka.streams.KafkaStreams} instance.
 */
public class KafkaStreamsMetricsCollector extends Collector {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamsMetricsCollector.class);

    private static final String LABEL_METRIC_GROUP = "group";
    private static final String LABEL_APPLICATION  = "id";
    private static final String METRIC_NAMESPACE   = "streams_";
    private static final String LABEL_VALUE = "value";

    private final AzkarraStreamsService service;

    private final String[] applications;

    private final Predicate<Tuple<String, Metric>> filter;

    /**
     * Creates a new {@link KafkaStreamsMetricsCollector} instance.
     *
     * @param service   the {@link AzkarraStreamsService} instance.
     */
    public KafkaStreamsMetricsCollector(final AzkarraStreamsService service,
                                        final Predicate<Tuple<String, Metric>> filter,
                                        final String... applications) {
        this.service = service;
        this.applications = applications;
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetricFamilySamples> collect() {

        List<MetricFamilySamples> samples = new LinkedList<>();
        for (String application : applications) {
            try {
                Set<MetricGroup> groupSet = service.getStreamsMetricsById(application, filter);

                samples.addAll(groupSet.stream()
                    .flatMap(group -> group.metrics().stream().map(m -> buildMetric(application, group.name(), m)))
                    .collect(Collectors.toList())
                );

            } catch (NotFoundException e) {
                LOG.error(e.getMessage());
            }
        }

        return samples;
    }

    private MetricFamilySamples buildMetric(final String application, final String group, final Metric metric) {
        List<String> labelNames = getLabelNamesFor(metric);
        final String metricName = METRIC_NAMESPACE + sanitize(metric.name());
        CounterMetricFamily family;
        List<String> labelValues = new ArrayList<>();
        labelValues.add(group);
        labelValues.add(application);
        labelValues.addAll(metric.tags().values());
        if (metric.value() instanceof Number) {
            family = new CounterMetricFamily(metricName, metric.description(), labelNames);
            family.addMetric(labelValues, ((Number) metric.value()).doubleValue());
        } else {
            labelNames.add(LABEL_VALUE);
            labelValues.add(metric.value().toString());
            family = new CounterMetricFamily(metricName, metric.description(), labelNames);
            family.addMetric(labelValues, 0);
        }
        return family;
    }

    private List<String> getLabelNamesFor(final Metric metric) {
        List<String> labelNames = new ArrayList<>();
        labelNames.add(LABEL_METRIC_GROUP);
        labelNames.add(LABEL_APPLICATION);
        List<String> tagNames = metric.tags().keySet()
            .stream()
            .map(KafkaStreamsMetricsCollector::sanitize)
            .collect(Collectors.toList());
        labelNames.addAll(tagNames);
        return labelNames;
    }

    private static String sanitize(final String metricName) {
        return metricName.replaceAll("-", "_");
    }
}

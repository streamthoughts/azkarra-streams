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
package io.streamthoughts.azkarra.metrics.interceptor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.annotations.ConditionalOn;
import io.streamthoughts.azkarra.api.components.BaseComponentModule;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.State;
import io.streamthoughts.azkarra.api.streams.StateChangeEvent;
import io.streamthoughts.azkarra.api.streams.internal.InternalStreamsLifecycleContext;
import io.streamthoughts.azkarra.metrics.AzkarraMetricsConfig;
import io.streamthoughts.azkarra.metrics.annotations.ConditionalOnMetricsEnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.streamthoughts.azkarra.metrics.AzkarraMetricsConfig.METRICS_BINDERS_KAFKA_STREAMS_ENABLE_CONFIG;

@Component
@ConditionalOnMetricsEnable
@ConditionalOn(property = METRICS_BINDERS_KAFKA_STREAMS_ENABLE_CONFIG, havingValue = "true")
public class MeterKafkaStreamsInterceptor extends BaseComponentModule implements StreamsLifecycleInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(MeterKafkaStreamsInterceptor.class);

    private static final String TAG_APPLICATION_ID = "application.id";

    private KafkaStreamsMetrics metrics;

    private boolean isEnable;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        super.configure(configuration);
        isEnable = new AzkarraMetricsConfig(getConfiguration()).isEnable(METRICS_BINDERS_KAFKA_STREAMS_ENABLE_CONFIG);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifecycleContext context,
                        final StreamsLifecycleChain chain) {
        LOG.info("Starting the MonitoringStreamsInterceptor for application = {}.", context.applicationId());
        if (isEnable) {
            context.addStateChangeWatcher(new KafkaStreamsContainer.StateChangeWatcher() {
                @Override
                public boolean accept(final State newState) {
                    return newState == State.RUNNING;
                }

                @Override
                public void onChange(final StateChangeEvent event) {
                    final List<Tag> tags = List.of(Tag.of(TAG_APPLICATION_ID, context.applicationId()));
                    final var kafkaStreams = ((InternalStreamsLifecycleContext) context).container().getKafkaStreams();
                    MeterRegistry registry = getComponent(MeterRegistry.class, Qualifiers.byPrimary());
                    metrics = new KafkaStreamsMetrics(kafkaStreams, tags);
                    metrics.bindTo(registry);
                    LOG.info(
                        "Bind metrics for application = {} to MeterRegistry[{}] successfully.",
                        context.applicationId(),
                        registry.getClass().getName());
                }
            });
        }
        chain.execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(final StreamsLifecycleContext context,
                       final StreamsLifecycleChain chain) {
        LOG.info("Closing the MonitoringStreamsInterceptor for application = {}.", context.applicationId());
        if (metrics != null) metrics.close();
        chain.execute();
    }
}

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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.StreamsLifecycleChain;
import io.streamthoughts.azkarra.api.StreamsLifecycleContext;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.model.TimestampedValue;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.StateChangeEvent;
import io.streamthoughts.azkarra.api.streams.internal.InternalStreamsLifecycleContext;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.KafkaStreamsMetadata;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.MonitoringStreamsTask;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Interceptor to monitor {@link org.apache.kafka.streams.KafkaStreams} instance.
 * Publishes periodically a streams state event to a Kafka topic.
 */
public class MonitoringStreamsInterceptor implements StreamsLifecycleInterceptor, Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(MonitoringStreamsInterceptor.class);

    private static final String PRODUCER_CLIENT_ID_SUFFIX = "-monitoring-producer";

    private final AtomicInteger taskGenerationId = new AtomicInteger(0);

    private MonitoringStreamsInterceptorConfig config;

    private Producer<byte[], byte[]> producer;

    private MonitoringStreamsTask task;

    private Supplier<MonitoringStreamsTask> taskSupplier;

    private KafkaStreamsContainer container;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        config = new MonitoringStreamsInterceptorConfig(configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifecycleContext context,
                        final StreamsLifecycleChain chain) {
        LOG.info("Starting the MonitoringStreamsInterceptor for application = {}.", context.applicationId());
        container = ((InternalStreamsLifecycleContext) context).container();
        container.addStateChangeWatcher(new ReporterStateChangeWatcher());

        final String clientId = context.applicationId() + PRODUCER_CLIENT_ID_SUFFIX;
        final Map<String, Object> producerConfigs = config.getProducerConfigs(clientId);
        producer = container.getProducer(producerConfigs);

        final String advertisedServer = config.getAdvertisedServer().orElse(container.applicationServer());
        taskSupplier = () -> new MonitoringStreamsTask(
            container.applicationId(),
            advertisedServer,
            config.getExtensions(),
            () -> new KafkaStreamsMetadata(
                container.state(),
                container.threadMetadata(),
                container.offsets()
            ),
            config.getIntervalMs(),
            producer,
            config.getTopic()
        );
        startTask(container.applicationId());
        chain.execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop(final StreamsLifecycleContext context,
                       final StreamsLifecycleChain chain) {
        try {
            chain.execute();
        } finally {
            final String id = container.applicationId();
            try {
                LOG.info("Closing MonitoringStreamsInterceptor for application = {}.", id);
                closeTasks();
                producer.flush();
            } catch (Exception e) {
                LOG.error("Unexpected error occurred while closing reporting task", e);
            } finally {
                producer.close();
                LOG.info("MonitoringStreamsInterceptor has been closed for application = {}.", id);
            }
        }
    }

    private void closeTasks() {
        if (task != null) {
            task.shutdown();
        }
    }

    private synchronized void startTask(final String applicationId) {
        LOG.info("(Re)creating a new task for monitoring the Kafka Streams application {}.", applicationId);
        final String threadName = applicationId + "-reporter-task-" + taskGenerationId.getAndIncrement();
        task = taskSupplier.get();
        task.setName(threadName);

        // We should immediately restart the reporting task in case of unexpected exception.
        task.setUncaughtExceptionHandler((t, e) -> {
            LOG.error("Unexpected error {} is DEAD ", t.getName(), e);
            startTask(applicationId);
        });
        task.start();
    }

    private class ReporterStateChangeWatcher implements KafkaStreamsContainer.StateChangeWatcher {

        @Override
        public void onChange(final StateChangeEvent event) {
            if (task != null) {
                task.offer(new KafkaStreamsMetadata(
                    new TimestampedValue<>(event.timestamp(), event.newState()),
                    container.threadMetadata(),
                    container.offsets()
                ));
            }
            container.addStateChangeWatcher(this);
        }
    }
}

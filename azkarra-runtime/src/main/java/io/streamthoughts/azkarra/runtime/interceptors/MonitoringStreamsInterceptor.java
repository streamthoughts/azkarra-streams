/*
 * Copyright 2019-2021 StreamThoughts.
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
import io.streamthoughts.azkarra.api.annotations.VisibleForTesting;
import io.streamthoughts.azkarra.api.components.BaseComponentModule;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.model.TimestampedValue;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainerAware;
import io.streamthoughts.azkarra.api.streams.StateChangeEvent;
import io.streamthoughts.azkarra.api.util.Utils;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.KafkaMonitoringReporter;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.KafkaStreamsMetadata;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.MonitoringStreamsTask;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.MonitoringReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig.MONITORING_INTERCEPTOR_INTERVAL_MS_DEFAULT;

/**
 * Interceptor to monitor {@link org.apache.kafka.streams.KafkaStreams} instance.
 * Publishes periodically a streams state event to a Kafka topic.
 */
public class MonitoringStreamsInterceptor
        extends BaseComponentModule
        implements StreamsLifecycleInterceptor, Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(MonitoringStreamsInterceptor.class);
    private final AtomicInteger taskGenerationId = new AtomicInteger(0);

    private MonitoringStreamsInterceptorConfig config;

    private MonitoringStreamsTask task;

    private Supplier<MonitoringStreamsTask> taskSupplier;

    private KafkaStreamsContainer container;

    private final List<MonitoringReporter> reporters = new ArrayList<>();

    private long intervalMs = MONITORING_INTERCEPTOR_INTERVAL_MS_DEFAULT;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        super.configure(configuration);

        config = new MonitoringStreamsInterceptorConfig(configuration);

        if (config.getIntervalMs().isPresent())
            intervalMs = config.getIntervalMs().get();

        // Gets and configure all configured reporters
        Collection<MonitoringReporter> configured = new ArrayList<>(config.getReporters());
        if (config.isKafkaReporterEnabled()) {
            configured.add(new KafkaMonitoringReporter());
        }
        for (MonitoringReporter reporter :configured) {
            Configurable.mayConfigure(reporter, configuration);
        }

        // Registers all configured reporters.
        this.reporters.addAll(configured);

        // Registers all component reporters
        this.reporters.addAll(getAllComponents(MonitoringReporter.class));
    }

    /**
     * Sets the interval in milliseconds to be used for reporting the state of the monitored Kafka Streams instance.
     * @param intervalMs    the interval in milliseconds.
     * @return              {@code this}.
     */
    public MonitoringStreamsInterceptor withIntervalMs(final long intervalMs) {
        this.intervalMs = intervalMs;
        return this;
    }

    /**
     * Registers a {@link MonitoringReporter} to this interceptor.
     *
     * @param reporter  the reporter to be registered.
     * @return          {@code this}.
     */
    public MonitoringStreamsInterceptor withMonitoringReporter(final MonitoringReporter reporter) {
        this.reporters.add(Objects.requireNonNull(reporter, "reporter should not be null"));
        return this;
    }

    @VisibleForTesting
    List<MonitoringReporter> reporters() {
        return reporters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(final StreamsLifecycleContext context,
                        final StreamsLifecycleChain chain) {
        LOG.info("Starting the MonitoringStreamsInterceptor for application = {}.", context.applicationId());

        container = context.container();
        container.addStateChangeWatcher(new ReporterStateChangeWatcher());

        for (MonitoringReporter reporter : reporters) {
            if (KafkaStreamsContainerAware.class.isAssignableFrom(reporter.getClass())) {
                ((KafkaStreamsContainerAware) reporter).setKafkaStreamsContainer(container);
            }
        }

        taskSupplier = () -> new MonitoringStreamsTask(
            container.applicationId(),
            reporters,
            () -> new KafkaStreamsMetadata(
                container.state(),
                container.threadMetadata(),
                container.offsets(),
                config.isStoresLagsEnabled() ? container.allLocalStorePartitionInfos() : Collections.emptyList()
            ),
            intervalMs
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
                closeReporters();
            } catch (Exception e) {
                LOG.error("Unexpected error occurred while closing reporting task", e);
            } finally {
                LOG.info("MonitoringStreamsInterceptor has been closed for application = {}.", id);
            }
        }
    }

    private void closeReporters() {
        reporters.forEach(Utils::closeQuietly);
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
                    container.offsets(),
                    config.isStoresLagsEnabled() ? container.allLocalStorePartitionInfos() : Collections.emptyList()
                ));
            }
            container.addStateChangeWatcher(this);
        }
    }
}

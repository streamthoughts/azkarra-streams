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
package io.streamthoughts.azkarra.runtime.interceptors.monitoring;

import io.streamthoughts.azkarra.api.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Task for reporting {@link org.apache.kafka.streams.KafkaStreams} state changes.
 */
public final class MonitoringStreamsTask extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(MonitoringStreamsTask.class);

    private final CountDownLatch isShutdownLatch;

    private final AtomicBoolean shutdown;

    private final long intervalMs;

    private long lastSentEventTimeMs = 0L;

    private final LinkedBlockingQueue<KafkaStreamsMetadata> changes;

    private final Reportable<? extends KafkaStreamsMetadata> reportable;

    private final List<MonitoringReporter> reporters;

    private final String applicationId;

    /**
     * Creates a new {@link MonitoringStreamsTask} instance.
     *
     * @param applicationId the application id.
     * @param reporters     the list of reporters to be used.
     * @param reportable    the {@link Reportable}.
     * @param intervalMs    the report interval in milliseconds.
     */
    public MonitoringStreamsTask(final String applicationId,
                                 final List<MonitoringReporter> reporters,
                                 final Reportable<? extends KafkaStreamsMetadata> reportable,
                                 final long intervalMs) {
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId should not be null");
        this.reportable = Objects.requireNonNull(reportable, "reportable should be null");
        this.reporters = Objects.requireNonNull(reporters, "reporters should not be null");
        this.intervalMs = intervalMs;
        this.isShutdownLatch = new CountDownLatch(1);
        this.shutdown = new AtomicBoolean(false);
        this. changes = new LinkedBlockingQueue<>();
    }

    public void offer(final KafkaStreamsMetadata state) {
        while (!changes.offer(state)) {
            try {
                pollAndReportOrWait(Duration.ZERO);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOG.info("Starting the StreamsStateReporterTask for application: {}", applicationId);
        try {
            while (!shutdown.get()) {
                final long start = Time.SYSTEM.milliseconds();
                // Check if we do need do send an event.
                if (maybeSendStatesEvent(start)) {
                    report(reportable.report());
                }
                try {
                    pollAndReportOrWait(shutdown.get() ? Duration.ZERO : timeUntilNextLoop());
                } catch (InterruptedException e) {
                    // This thread has been interrupted while waiting for changes; stop reporting.
                    // Set shutdown to true to end the loop.
                    shutdown.set(true);
                }
            }
        } catch (Exception e) {
            LOG.error(
                "Unexpected error while reporting state for streams application : {}. Stopping task.",
                applicationId,
                e
            );
            shutdown.set(true);
        } finally {
            // Closing or flushing the producer is not the responsibility of this Task.
            isShutdownLatch.countDown();
            LOG.info("StreamsStateReporterTask has been stopped for application: {}", applicationId);
        }
    }

    private void pollAndReportOrWait(final Duration duration) throws InterruptedException {
        final KafkaStreamsMetadata change = changes.poll(duration.toMillis(), TimeUnit.MILLISECONDS);
        if (change != null) {
            report(change);
        }
    }

    private void report(final KafkaStreamsMetadata metadata) {
        for (MonitoringReporter reporter : reporters) {
            try {
                reporter.report(metadata);
            } catch (Exception e) {
                LOG.error(
                    "Failed to report KafkaStreams metadata using reporter '{}'",
                    reporter.getClass().getSimpleName(),
                    e
                );
            }
        }
        lastSentEventTimeMs = Time.SYSTEM.milliseconds();
    }

    private Duration timeUntilNextLoop() {
        long now = Time.SYSTEM.milliseconds();
        return Duration.ofMillis(Math.max(intervalMs - (now - lastSentEventTimeMs), 0));
    }

    private boolean maybeSendStatesEvent(long now) {
        return now - lastSentEventTimeMs >= intervalMs;
    }

    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            try {
                isShutdownLatch.await(intervalMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public interface Reportable<T> {

        /**
         * Gets an object to report.
         *
         * @return  the object to report.
         */
        T report();
    }

}

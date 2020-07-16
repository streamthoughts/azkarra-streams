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
package io.streamthoughts.azkarra.runtime.interceptors.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.streamthoughts.azkarra.api.time.Time;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.ce.CloudEventsBuilder;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.ce.CloudEventsEntity;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.ce.CloudEventsExtension;
import io.streamthoughts.azkarra.serialization.json.AzkarraSimpleModule;
import io.streamthoughts.azkarra.serialization.json.Json;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

    private static final Json JSON = new Json(new ObjectMapper());
    private static final String DEFAULT_EVENT_TYPE = "io.streamthoughts.azkarra.streams.stateupdateevent";
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String CE_SPEC_VERSION = "1.0";

    private static final Headers RECORD_HEADERS = new RecordHeaders()
            .add("content-type", "application/cloudevents+json; charset=UTF-8".getBytes(StandardCharsets.UTF_8));

    static {
        JSON.registerModule(new AzkarraSimpleModule());
        JSON.registerModule(new JavaTimeModule());
        JSON.configure(o -> o.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true));
    }

    private final CountDownLatch isShutdownLatch;

    private final AtomicBoolean shutdown;

    private final long intervalMs;

    private long lastSentEventTimeMs = 0L;

    private volatile LinkedBlockingQueue<KafkaStreamsMetadata> changes;

    private final CloudEventsContext evtContext;

    private final String topic;

    private final Producer<byte[], byte[]> producer;

    private final CloudEventsExtension azkarraExtensions;

    private final CloudEventsExtension customExtensions;

    private final Reportable<? extends KafkaStreamsMetadata> reportable;

    private final byte[] key;

    /**
     * Creates a new {@link MonitoringStreamsTask} instance.
     */
    public MonitoringStreamsTask(final CloudEventsContext evtContext,
                                 final CloudEventsExtension customExtensions,
                                 final Reportable<? extends KafkaStreamsMetadata> reportable,
                                 final long intervalMs,
                                 final Producer<byte[], byte[]> producer,
                                 final String topic) {
        this.evtContext = Objects.requireNonNull(evtContext, "evtContext can't be null");
        this.reportable = Objects.requireNonNull(reportable, "reportable can't be null");
        this.producer = Objects.requireNonNull(producer, "producer can't be null");
        this.topic = Objects.requireNonNull(topic, "topic can't be null");
        this.intervalMs = intervalMs;
        this.isShutdownLatch = new CountDownLatch(1);
        this.shutdown = new AtomicBoolean(false);
        this.customExtensions = customExtensions;
        changes = new LinkedBlockingQueue<>();
        azkarraExtensions = StreamsExtensionBuilder.newBuilder()
            .withApplicationId(evtContext.applicationId())
            .withApplicationServer(evtContext.applicationServer())
            .with("monitorintervalms", intervalMs)
            .build();

        key = evtContext.applicationId().getBytes(StandardCharsets.UTF_8);
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
        LOG.info("Starting the StreamsStateReporterTask for application: {}", evtContext.applicationId());
        try {
            while (!shutdown.get()) {
                final long start = Time.SYSTEM.milliseconds();
                // Check if we do need do send an event.
                if (maybeSendStatesEvent(start)) {
                    report(buildEvent(reportable.report()));
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
                    evtContext.applicationId(),
                e
            );
            shutdown.set(true);
        } finally {
            // Closing or flushing the producer is not the responsibility of this Task.
            isShutdownLatch.countDown();
            LOG.info("StreamsStateReporterTask has been stopped for application: {}", evtContext.applicationId());
        }
    }

    private void pollAndReportOrWait(final Duration duration) throws InterruptedException {
        final KafkaStreamsMetadata change = changes.poll(duration.toMillis(), TimeUnit.MILLISECONDS);
        if (change != null) {
            report(buildEvent(change));
        }
    }

    private Duration timeUntilNextLoop() {
        long now = Time.SYSTEM.milliseconds();
        return Duration.ofMillis(Math.max(intervalMs - (now - lastSentEventTimeMs), 0));
    }

    private boolean maybeSendStatesEvent(long now) {
        return now - lastSentEventTimeMs >= intervalMs;
    }

    private CloudEventsEntity<KafkaStreamsMetadata> buildEvent(final KafkaStreamsMetadata data) {
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return CloudEventsBuilder.<KafkaStreamsMetadata>newBuilder()
            .withId(evtContext.cloudEventId(now))
            .withSource(evtContext.cloudEventSource())
            .withSubject(evtContext.cloudEventSubject())
            .withType(DEFAULT_EVENT_TYPE)
            .withSpecVersion(CE_SPEC_VERSION)
            .withTime(now)
            .withData(data)
            .withDataContentType(DEFAULT_CONTENT_TYPE)
            .withExtension(azkarraExtensions)
            .withExtension(customExtensions)
            .build();
    }


    private void report(final CloudEventsEntity<KafkaStreamsMetadata> event) {
        final byte[] byteValue = JSON.serialize(event).getBytes(StandardCharsets.UTF_8);
        producer.send(new ProducerRecord<>(topic, null, key, byteValue, RECORD_HEADERS));
        lastSentEventTimeMs = event.attributes().time().toInstant().toEpochMilli();
    }

    public void shutdown() {
        if (shutdown.get()) return;
        //interrupt(); // interrupt the thread to not wait for new changes.
        shutdown.set(true);
        try {
            isShutdownLatch.await(intervalMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
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

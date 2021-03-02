/*
 * Copyright 2021 StreamThoughts.
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
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainerAware;
import io.streamthoughts.azkarra.api.util.Endpoint;
import io.streamthoughts.azkarra.runtime.interceptors.MonitoringStreamsInterceptorConfig;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.ce.CloudEventsBuilder;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.ce.CloudEventsContext;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link MonitoringReporter} that send a serialized {@link KafkaStreamsMetadata} into a Kafka topic.
 */
public class KafkaMonitoringReporter implements MonitoringReporter, KafkaStreamsContainerAware {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaMonitoringReporter.class);

    private static final String PRODUCER_CLIENT_ID_SUFFIX = "-monitoring-producer";

    private static final String DEFAULT_EVENT_TYPE = "io.streamthoughts.azkarra.streams.stateupdateevent";
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String CE_SPEC_VERSION = "1.0";

    private static final Headers RECORD_HEADERS = new RecordHeaders()
            .add("content-type", "application/cloudevents+json; charset=UTF-8".getBytes(StandardCharsets.UTF_8));

    private static final Json JSON = new Json(new ObjectMapper());

    static {
        JSON.registerModule(new AzkarraSimpleModule());
        JSON.registerModule(new JavaTimeModule());
        JSON.configure(o -> o.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true));
    }

    private KafkaStreamsContainer container;

    private CloudEventsContext context;

    private Producer<byte[], byte[]> producer;

    private CloudEventsExtension azkarraExtensions;

    private byte[] key;

    private MonitoringStreamsInterceptorConfig config;

    private CloudEventsExtension customExtensions;

    private String topic = MonitoringStreamsInterceptorConfig.MONITORING_INTERCEPTOR_TOPIC_DEFAULT;

    private String applicationServer;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * {@inheritDoc}
     */
    @Override
    public void report(final KafkaStreamsMetadata metadata) {

        initialized(); // ensure the reporter is configured.

        final CloudEventsEntity<KafkaStreamsMetadata> event = buildEvent(metadata);
        final byte[] byteValue = JSON.serialize(event).getBytes(StandardCharsets.UTF_8);
        producer.send(new ProducerRecord<>(topic, null, key, byteValue, RECORD_HEADERS));
    }

    private CloudEventsEntity<KafkaStreamsMetadata> buildEvent(final KafkaStreamsMetadata data) {
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return CloudEventsBuilder.<KafkaStreamsMetadata>newBuilder()
                .withId(context.cloudEventId(now))
                .withSource(context.cloudEventSource())
                .withSubject(context.cloudEventSubject())
                .withType(DEFAULT_EVENT_TYPE)
                .withSpecVersion(CE_SPEC_VERSION)
                .withTime(now)
                .withData(data)
                .withDataContentType(DEFAULT_CONTENT_TYPE)
                .withExtension(azkarraExtensions)
                .withExtension(customExtensions)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Conf configuration) {
        config = new MonitoringStreamsInterceptorConfig(configuration);

        if (config.getTopic().isPresent()) {
            topic = config.getTopic().get();
        }

        if (config.getAdvertisedServer().isPresent()) {
            applicationServer = config.getAdvertisedServer().get();
        }

        customExtensions = config.getExtensions();
    }

    public KafkaMonitoringReporter withTopic(final String topic) {
        this.topic = topic;
        return this;
    }

    public KafkaMonitoringReporter withAdvertisedApplicationServer(final String advertisedServer) {
        this.applicationServer = advertisedServer;
        return this;
    }

    private void initialized() {
        if (initialized.compareAndSet(false, true)) {
            applicationServer = Optional.ofNullable(applicationServer)
                .or(() -> container.endpoint().map(Endpoint::listener))
                .orElse("-");

            context = new CloudEventsContext(
                    container.applicationId(),
                    applicationServer,
                    queryClusterId()
            );
            azkarraExtensions = StreamsExtensionBuilder.newBuilder()
                    .withApplicationId(context.applicationId())
                    .withApplicationServer(context.applicationServer())
                    .with("monitorintervalms", config.getIntervalMs())
                    .build();

            key = context.applicationId().getBytes(StandardCharsets.UTF_8);

            final String clientId = container.applicationId() + PRODUCER_CLIENT_ID_SUFFIX;
            final Map<String, Object> producerConfigs = config.getProducerConfigs(clientId);
            producer = container.createNewProducer(producerConfigs);
        }
    }

    private String queryClusterId() {
        try {
            return container.getAdminClient().describeCluster().clusterId().get();
        } catch (Exception e) {
            LOG.error("Failed to describe cluster Id. Use default value 'unknown'", e);
            return "unknown";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKafkaStreamsContainer(final KafkaStreamsContainer container) {
        this.container = container;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        producer.close();
        producer = null;
        initialized.set(false);
    }
}

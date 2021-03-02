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
package io.streamthoughts.azkarra.runtime.interceptors;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.MonitoringReporter;
import io.streamthoughts.azkarra.runtime.interceptors.monitoring.ce.CloudEventsExtension;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.record.CompressionType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The configuration class for {@link MonitoringStreamsInterceptor}.
 */
public class MonitoringStreamsInterceptorConfig {

    /** {@code monitoring.streams.interceptor.enable} */
    public static String MONITORING_STREAMS_INTERCEPTOR_ENABLE_CONFIG = "monitoring.streams.interceptor.enable";

    /**
     * Prefix used to isolate {@link KafkaProducer producer} configs from other client configs.
     * It is recommended to use {@link #producerPrefix(String)} to add this prefix to {@link ProducerConfig producer
     * properties}.
     */
    public static final String MONITORING_INTERCEPTOR_PRODUCER_PREFIX = "monitoring.streams.interceptor.producer";

    /** {@code monitoring.streams.interceptor.interval.ms} */
    public static final String MONITORING_INTERCEPTOR_INTERVAL_MS_CONFIG = "monitoring.streams.interceptor.interval.ms";
    public static final long MONITORING_INTERCEPTOR_INTERVAL_MS_DEFAULT = 10000;

    /** {@code monitoring.streams.interceptor.topic} */
    public static final String MONITORING_INTERCEPTOR_TOPIC_CONFIG = "monitoring.streams.interceptor.topic";
    public static final String MONITORING_INTERCEPTOR_TOPIC_DEFAULT = "_azkarra-streams-monitoring";

    /** {@code monitoring.streams.interceptor.advertised.server} */
    public static final String MONITORING_INTERCEPTOR_ADVERTISED_SERVER_CONFIG = "monitoring.streams.interceptor.advertised.server";

    /** {@code monitoring.streams.interceptor.info.enabled.stores.lag} */
    public static final String MONITORING_INTERCEPTOR_ENABLE_STORES_LAG_CONFIG = "monitoring.streams.interceptor.info.enabled.stores.lag";

    /** {@code monitoring.streams.interceptor.ce.extensions} */
    public static final String MONITORING_INTERCEPTOR_EXTENSIONS_CONFIG = "monitoring.streams.interceptor.ce.extensions";

    /** {@code monitoring.streams.interceptor.info.enabled.stores.lag} */
    public static final String MONITORING_INTERCEPTOR_REPORTERS_CLASSES_CONFIG = "monitoring.streams.interceptor.reporters";

    /** {@code monitoring.streams.interceptor.enable} */
    public static String MONITORING_STREAMS_INTERCEPTOR_KAFKA_REPORTER_ENABLE_CONFIG = "monitoring.streams.interceptor.kafka.reporter.enabled";

    private static final Map<String, Object> PRODUCER_DEFAULT_OVERRIDES = Map.of(
        ProducerConfig.LINGER_MS_CONFIG, "100",
        ProducerConfig.ACKS_CONFIG, "1",
        ProducerConfig.RETRIES_CONFIG, "0",
        ProducerConfig.COMPRESSION_TYPE_CONFIG, CompressionType.LZ4.name,
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false,
        ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1"
    );

    private final Conf originals;

    /**
     * Creates a new {@link MonitoringStreamsInterceptorConfig} instance.
     *
     * @param originals the {@link Conf} instance.
     */
    public MonitoringStreamsInterceptorConfig(final Conf originals) {
        this.originals = originals;
    }

    /**
     * Prefix a property with {@link #MONITORING_INTERCEPTOR_PRODUCER_PREFIX}. This is used to isolate {@link ProducerConfig producer configs}.
     *
     * @param producerProp the producer property to be masked
     * @return WATCHER_PRODUCER_PREFIX + {@code producerProp}
     */
    public static String producerPrefix(final String producerProp) {
        return MONITORING_INTERCEPTOR_PRODUCER_PREFIX + "." + producerProp;
    }

    public boolean isKafkaReporterEnabled() {
        return originals.getOptionalBoolean(MONITORING_STREAMS_INTERCEPTOR_KAFKA_REPORTER_ENABLE_CONFIG).orElse(true);
    }

    public Collection<MonitoringReporter> getReporters() {
        if (!originals.hasPath(MONITORING_INTERCEPTOR_REPORTERS_CLASSES_CONFIG))
            return Collections.emptyList();

        return originals.getClasses(MONITORING_INTERCEPTOR_REPORTERS_CLASSES_CONFIG, MonitoringReporter.class);
    }

    /**
     * Get the period the interceptor should use to send a streams state event (Default is 10 seconds).
     *
     * @return the period in milliseconds.
     */
    public Optional<Long> getIntervalMs() {
        return originals
            .getOptionalLong(MONITORING_INTERCEPTOR_INTERVAL_MS_CONFIG);
    }

    /**
     * Get the server name that will be included in monitoring events.
     * If not specified, the streams <code>application.server</code> property is used."
     *
     * @return  an optional advertised server name.
     */
    public Optional<String> getAdvertisedServer() {
        return originals.getOptionalString(MONITORING_INTERCEPTOR_ADVERTISED_SERVER_CONFIG);
    }

    /**
     * Get the topic on which monitoring event will be sent (Default is _azkarra-streams-monitoring).
     *
     * @return  the name of the topic.
     */
    public Optional<String> getTopic() {
        return originals.getOptionalString(MONITORING_INTERCEPTOR_TOPIC_CONFIG);
    }

    /**
     * Get the list of extension attributes that should be included in monitoring events.
     *
     * @return  the {@link CloudEventsExtension}.
     */
    public CloudEventsExtension getExtensions() {
        final Map<String, Object> extensions = originals.hasPath(MONITORING_INTERCEPTOR_EXTENSIONS_CONFIG) ?
                originals.getSubConf(MONITORING_INTERCEPTOR_EXTENSIONS_CONFIG).getConfAsMap() :
                Collections.emptyMap();
        return () -> extensions;
    }

    /**
     * Checks if offset lags should be monitored for all local state stores.
     *
     * @return {@code false} by default.
     */
    public boolean isStoresLagsEnabled() {
        return originals
            .getOptionalBoolean(MONITORING_INTERCEPTOR_ENABLE_STORES_LAG_CONFIG)
            .orElse(false);
    }

    /**
     * Get the configs for the {@link KafkaProducer producer}.
     * Properties using the prefix {@link #MONITORING_INTERCEPTOR_PRODUCER_PREFIX} will be used in favor over their non-prefixed versions
     *.
     * @param clientId clientId
     * @return Map of the producer configuration.
     */
    public Map<String, Object> getProducerConfigs(final String clientId) {

        final Map<String, Object> producerConfigs = new HashMap<>(PRODUCER_DEFAULT_OVERRIDES);
        producerConfigs.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId);

        if (!originals.hasPath(MONITORING_INTERCEPTOR_PRODUCER_PREFIX))
            return producerConfigs;

        Map<String, Object> overrides = originals.getSubConf(MONITORING_INTERCEPTOR_PRODUCER_PREFIX).getConfAsMap();
        producerConfigs.putAll(clientProps(ProducerConfig.configNames(),overrides));

        return producerConfigs;
    }

    private Map<String, Object> clientProps(final Set<String> configNames,
                                            final Map<String, Object> originals) {
        final Map<String, Object> parsed = new HashMap<>();
        configNames.stream()
            .filter(originals::containsKey)
            .forEach(configName -> parsed.put(configName, originals.get(configName)));

        return parsed;
    }
}
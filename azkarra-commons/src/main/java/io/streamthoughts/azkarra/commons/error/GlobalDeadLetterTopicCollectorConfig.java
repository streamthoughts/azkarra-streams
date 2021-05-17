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
package io.streamthoughts.azkarra.commons.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

/**
 * The {@code GlobalDeadLetterTopicCollectorConfig} is used to initialize the
 * {@link GlobalDeadLetterTopicCollector} instance.
 *
 * @see GlobalDeadLetterTopicCollector#getOrCreate(GlobalDeadLetterTopicCollectorConfig).
 */
public class GlobalDeadLetterTopicCollectorConfig extends AbstractConfig {

    private static final String GROUP = "Global Dead Letter Topic Collector";

    public static final String DLQ_GLOBAL_PRODUCER_PREFIX_CONFIG =
            "exception.handler.dlq.global.producer.";

    public static final String DLQ_GLOBAL_ADMIN_PREFIX_CONFIG =
            "exception.handler.dlq.global.admin.";

    public static final String DLQ_AUTO_CREATE_TOPIC_ENABLED_CONFIG
            = "exception.handler.dlq.topics.auto.create.enabled";
    private static final String DLQ_AUTO_CREATE_TOPIC_ENABLED_DOC
            = "If set to true, missing DLQ topic are automatically created.";

    public static final String DLQ_AUTO_CREATE_TOPIC_PARTITIONS_CONFIG
            = "exception.handler.dlq.topics.partitions";
    private static final String DLQ_AUTO_CREATE_TOPIC_PARTITIONS_DOC
            = "The number of partitions to be used for DLQ topics.";

    public static final String DLQ_AUTO_CREATE_TOPIC_REPLICATION_CONFIG
            = "exception.handler.dlq.topics.replication.factor";

    private static final String DLQ_AUTO_CREATE_TOPIC_REPLICATION_DOC
            = "The replication factor to be used for DLQ topics.";

    private Producer<byte[], byte[]> producer;

    private AdminClient adminClient;

    private boolean isAutoCreateTopicEnabled = true;

    private Integer topicPartitions = null;

    private Short replicationFactor = null;

    private Map<String, Object> producerConfig = new HashMap<>();

    private Map<String, Object> adminClientConfig = new HashMap<>();

    /**
     * Creates a new {@link GlobalDeadLetterTopicCollectorConfig} instance.
     *
     * @param originals the configuration.
     */
    public GlobalDeadLetterTopicCollectorConfig(final Map<String, ?> originals) {
        super(configDef(), originals, false);
        withAutoCreateTopicEnabled(getBoolean(DLQ_AUTO_CREATE_TOPIC_ENABLED_CONFIG));
        withTopicPartitions(getInt(DLQ_AUTO_CREATE_TOPIC_PARTITIONS_CONFIG));
        withReplicationFactor(getShort(DLQ_AUTO_CREATE_TOPIC_REPLICATION_CONFIG));

        final HashMap<String, Object> producerConfigs = new HashMap<>();
        producerConfigs.putAll(getProducerConfigs(originals()));
        producerConfigs.putAll(originalsWithPrefix(DLQ_GLOBAL_PRODUCER_PREFIX_CONFIG));
        withProducerConfig(producerConfigs);

        final HashMap<String, Object> adminConfigs = new HashMap<>();
        adminConfigs.putAll(getAdminClientConfigs(originals()));
        adminConfigs.putAll(originalsWithPrefix(DLQ_GLOBAL_ADMIN_PREFIX_CONFIG));
        withAdminClientConfig(adminConfigs);
    }

    /**
     * @return a new {@link GlobalDeadLetterTopicCollectorConfig}.
     */
    public static GlobalDeadLetterTopicCollectorConfig create() {
        return new GlobalDeadLetterTopicCollectorConfig(new HashMap<>());
    }

    public GlobalDeadLetterTopicCollectorConfig withProducer(final Producer<byte[], byte[]> producer) {
        this.producer = producer;
        return this;
    }

    public GlobalDeadLetterTopicCollectorConfig withAdminClient(final AdminClient adminClient) {
        this.adminClient = adminClient;
        return this;
    }

    public GlobalDeadLetterTopicCollectorConfig withProducerConfig(
            final Map<String, Object> producerConfig) {
        this.producerConfig = producerConfig;
        return this;
    }

    public GlobalDeadLetterTopicCollectorConfig withAdminClientConfig(
            final Map<String, Object> adminClientConfig) {
        this.adminClientConfig = adminClientConfig;
        return this;
    }

    public GlobalDeadLetterTopicCollectorConfig withAutoCreateTopicEnabled(
            final boolean isAutoCreateTopicEnabled) {
        this.isAutoCreateTopicEnabled = isAutoCreateTopicEnabled;
        return this;
    }

    public GlobalDeadLetterTopicCollectorConfig withTopicPartitions(final Integer topicPartitions) {
        this.topicPartitions = topicPartitions;
        return this;
    }

    public GlobalDeadLetterTopicCollectorConfig withReplicationFactor(final Short replicationFactor) {
        this.replicationFactor = replicationFactor;
        return this;
    }

    /**
     * @return the {@link Producer} to be used by the {@link GlobalDeadLetterTopicCollector}.
     */
    Optional<Producer<byte[], byte[]>> getProducer() {
        return Optional.ofNullable(producer);
    }

    /**
     * @return the {@link AdminClient} to be used by the {@link GlobalDeadLetterTopicCollector}.
     */
    Optional<AdminClient> getAdminClient() {
        return Optional.ofNullable(adminClient);
    }

    /**
     * @return the config properties to be used by the {@link GlobalDeadLetterTopicCollector} for creating a new
     * {@link Producer} if no one is configured.
     */
    Map<String, Object> getProducerConfig() {
        return new HashMap<>(producerConfig);
    }

    /**
     * @return the config properties to be used by the {@link GlobalDeadLetterTopicCollector} for creating a new
     * {@link AdminClient} if no one is configured.
     */
    Map<String, Object> getAdminClientConfig() {
        return new HashMap<>(adminClientConfig);
    }

    /**
     * @return {@code true} if the  {@link GlobalDeadLetterTopicCollector} should create missing topics.
     */
    boolean isAutoCreateTopicEnabled() {
        return isAutoCreateTopicEnabled;
    }

    /**
     * @return the default number of partitions to be used for creating dead-letter-topic.
     */
    Optional<Integer> getTopicPartitions() {
        return Optional.ofNullable(topicPartitions);
    }

    /**
     * @return the default replication factor to be used for creating dead-letter-topic.
     */
    Optional<Short> getReplicationFactor() {
        return Optional.ofNullable(replicationFactor);
    }

    private static Map<String, Object> getAdminClientConfigs(final Map<String, Object> configs) {
        return getConfigsForKeys(configs, AdminClientConfig.configNames());
    }

    private static Map<String, Object> getProducerConfigs(final Map<String, Object> configs) {
        return getConfigsForKeys(configs, ProducerConfig.configNames());
    }

    private static Map<String, Object> getConfigsForKeys(
            final Map<String, Object> configs, final Set<String> keys) {
        final Map<String, Object> parsed = new HashMap<>();
        for (final String configName : keys) {
            if (configs.containsKey(configName)) {
                parsed.put(configName, configs.get(configName));
            }
        }
        return parsed;
    }

    private static ConfigDef configDef() {
        int orderInGroup = 0;
        return new ConfigDef()
                .define(
                        DLQ_AUTO_CREATE_TOPIC_ENABLED_CONFIG,
                        ConfigDef.Type.BOOLEAN,
                        true,
                        ConfigDef.Importance.HIGH,
                        DLQ_AUTO_CREATE_TOPIC_ENABLED_DOC,
                        GROUP,
                        orderInGroup++,
                        ConfigDef.Width.NONE,
                        DLQ_AUTO_CREATE_TOPIC_ENABLED_CONFIG
                )
                .define(
                        DLQ_AUTO_CREATE_TOPIC_PARTITIONS_CONFIG,
                        ConfigDef.Type.INT,
                        null,
                        ConfigDef.Importance.HIGH,
                        DLQ_AUTO_CREATE_TOPIC_PARTITIONS_DOC,
                        GROUP,
                        orderInGroup++,
                        ConfigDef.Width.NONE,
                        DLQ_AUTO_CREATE_TOPIC_PARTITIONS_CONFIG
                )
                .define(
                        DLQ_AUTO_CREATE_TOPIC_REPLICATION_CONFIG,
                        ConfigDef.Type.SHORT,
                        null,
                        ConfigDef.Importance.HIGH,
                        DLQ_AUTO_CREATE_TOPIC_REPLICATION_DOC,
                        GROUP,
                        orderInGroup++,
                        ConfigDef.Width.NONE,
                        DLQ_AUTO_CREATE_TOPIC_REPLICATION_CONFIG
                );
    }
}

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

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.Producer;

/**
 * The {@code GlobalDeadLetterTopicManagerConfig} is used to initialize the
 * {@link GlobalDeadLetterTopicManager} instance.
 *
 * @see GlobalDeadLetterTopicManager#initialize(GlobalDeadLetterTopicManagerConfig).
 */
public class GlobalDeadLetterTopicManagerConfig {

    private Producer<byte[], byte[]> producer;

    private AdminClient adminClient;

    private boolean isAutoCreateTopicEnabled = true;

    private Integer topicPartitions = null;

    private Short replicationFactor = null;

    private Map<String, Object> producerConfig = new HashMap<>();

    private Map<String, Object> adminClientConfig = new HashMap<>();

    /**
     * @return a new {@link GlobalDeadLetterTopicManagerConfig}.
     */
    public static GlobalDeadLetterTopicManagerConfig create() {
        return new GlobalDeadLetterTopicManagerConfig();
    }

    public GlobalDeadLetterTopicManagerConfig withProducer(final Producer<byte[], byte[]> producer) {
        this.producer = producer;
        return this;
    }

    public GlobalDeadLetterTopicManagerConfig withAdminClient(final AdminClient adminClient) {
        this.adminClient = adminClient;
        return this;
    }

    public GlobalDeadLetterTopicManagerConfig withProducerConfig(
            final Map<String, Object> producerConfig) {
        this.producerConfig = producerConfig;
        return this;
    }

    public GlobalDeadLetterTopicManagerConfig withAdminClientConfig(
            final Map<String, Object> adminClientConfig) {
        this.adminClientConfig = adminClientConfig;
        return this;
    }

    public GlobalDeadLetterTopicManagerConfig withAutoCreateTopicEnabled(
            final boolean isAutoCreateTopicEnabled) {
        this.isAutoCreateTopicEnabled = isAutoCreateTopicEnabled;
        return this;
    }

    public GlobalDeadLetterTopicManagerConfig withTopicPartitions(final Integer topicPartitions) {
        this.topicPartitions = topicPartitions;
        return this;
    }

    public GlobalDeadLetterTopicManagerConfig withReplicationFactor(final Short replicationFactor) {
        this.replicationFactor = replicationFactor;
        return this;
    }

    public Optional<Producer<byte[], byte[]>> getProducer() {
        return Optional.ofNullable(producer);
    }

    public Optional<AdminClient> getAdminClient() {
        return Optional.ofNullable(adminClient);
    }

    public Map<String, Object> getProducerConfig() {
        return new HashMap<>(producerConfig);
    }

    public Map<String, Object> getAdminClientConfig() {
        return new HashMap<>(adminClientConfig);
    }

    public boolean isAutoCreateTopicEnabled() {
        return isAutoCreateTopicEnabled;
    }

    public Optional<Integer> getTopicPartitions() {
        return Optional.ofNullable(topicPartitions);
    }

    public Optional<Short> getReplicationFactor() {
        return Optional.ofNullable(replicationFactor);
    }
}

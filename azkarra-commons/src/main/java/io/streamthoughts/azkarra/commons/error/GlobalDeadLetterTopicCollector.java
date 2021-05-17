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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code GlobalDeadLetterTopicCollector} can be used for sending corrupted record to Dead Letter Topics.
 */
public class GlobalDeadLetterTopicCollector implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalDeadLetterTopicCollector.class);

    public static final String DEFAULT_CLIENT_ID = "kafka-streams-global-dlq-producer";

    private static volatile GlobalDeadLetterTopicCollector INSTANCE;

    private static final AtomicBoolean CONFIGURED = new AtomicBoolean(false);

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final Set<String> topicCache = ConcurrentHashMap.newKeySet();

    private final GlobalDeadLetterTopicCollectorConfig config;

    private final Producer<byte[], byte[]> producer;

    private final AdminClient adminClient;

    /**
     * Creates a new {@link GlobalDeadLetterTopicCollector} instance.
     *
     * @param config               the {@link GlobalDeadLetterTopicCollectorConfig}.
     * @param registerShutdownHook flag to indicate if a register shutdown hook should be registered.
     */
    private GlobalDeadLetterTopicCollector(final GlobalDeadLetterTopicCollectorConfig config,
                                           final boolean registerShutdownHook) {
        this.config = config;
        this.producer = config.getProducer().get();
        this.adminClient = config.getAdminClient().orElse(null);
        mayRegisterShutdownHook(registerShutdownHook);
    }

    private void mayRegisterShutdownHook(final boolean registerShutdownHook) {
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
            LOG.info("Registered a JVM shutdown hook for closing GlobalDeadLetterTopicCollector.");
        }
    }

    private static KafkaProducer<byte[], byte[]> createProducer(final Map<String, Object> config) {
        return new KafkaProducer<>(config, new ByteArraySerializer(), new ByteArraySerializer());
    }

    private static AdminClient createAdminClient(final Map<String, Object> config) {
        return KafkaAdminClient.create(config);
    }

    /**
     * Gets the {@link GlobalDeadLetterTopicCollector} and create it if no one is already initialized.
     *
     * @param config the {@link GlobalDeadLetterTopicCollectorConfig}.
     * @return the {@link GlobalDeadLetterTopicCollector} instance.
     */
    public static synchronized GlobalDeadLetterTopicCollector getOrCreate(
            final GlobalDeadLetterTopicCollectorConfig config
    ) {
        if (CONFIGURED.compareAndSet(false, true)) {
            if (config.getProducer().isEmpty()) {
                LOG.info("Initializing global-dead-letter-topic-producer using the supplied configuration.");
                final Map<String, Object> newProducerConfig = config.getProducerConfig();
                newProducerConfig.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
                newProducerConfig.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
                newProducerConfig.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, DEFAULT_CLIENT_ID);
                config.withProducer(createProducer(newProducerConfig));
            }

            if (config.isAutoCreateTopicEnabled() && config.getAdminClient().isEmpty()) {
                LOG.info("Initializing global-dead-letter-topic-admin-client using the supplied configuration.");
                config.withAdminClient(createAdminClient(config.getAdminClientConfig()));
            }

            INSTANCE = new GlobalDeadLetterTopicCollector(config, true);
        }
        return INSTANCE;
    }

    /**
     * @return {@code true} if the {@link GlobalDeadLetterTopicCollector} is created.
     */
    public static synchronized boolean isCreated() {
        return CONFIGURED.get();
    }

    /**
     * Gets the {@link GlobalDeadLetterTopicCollector}.
     *
     * @return the {@link GlobalDeadLetterTopicCollector}.
     * @throws IllegalStateException if no {@link GlobalDeadLetterTopicCollector} has been created.
     */
    public static synchronized GlobalDeadLetterTopicCollector get() {
        if (!CONFIGURED.get()) {
            throw new IllegalStateException("GlobalDeadLetterTopicCollector is not created.");
        }
        return INSTANCE;
    }

    /**
     * Sends a {@code null} key-value record to a dead-letter topics using the given context information.
     *
     * @param topic     the name of the Dead Letter Topic.
     * @param failed    the {@link Failed} record context.
     */
    public void send(final String topic,
                     final Failed failed) {
        send(topic, null, null, null, null, failed);
    }

    /**
     * Sends a key-value record to a dead-letter topics using the given context information.
     *
     * @param topic             the name of the Dead Letter Topic.
     * @param key               the record-key.
     * @param value             the record-value.
     * @param keySerializer     the {@link Serializer} for the key.
     * @param valueSerializer   the {@link Serializer} for the value.
     * @param failed            the {@link Failed} record context.
     * @param <K>               the type of the key.
     * @param <V>               the type of the value.
     */
    public <K, V> void send(final String topic,
                            final K key,
                            final V value,
                            final Serializer<K> keySerializer,
                            final Serializer<V> valueSerializer,
                            final Failed failed) {

        final Headers enrichedHeaders = ExceptionHeaders.addExceptionHeaders(failed.headers(), failed);

        final byte[] keyBytes = Optional.ofNullable(key)
                .map(o -> keySerializer.serialize(topic, key))
                .orElse(null);

        final byte[] valueBytes = Optional.ofNullable(key)
                .map(o -> valueSerializer.serialize(topic, value))
                .orElse(null);

        final ProducerRecord<byte[], byte[]> record =
                new ProducerRecord<>(
                        topic,
                        null,
                        failed.recordTimestamp().orElse(null),
                        keyBytes,
                        valueBytes,
                        enrichedHeaders
                );
        send(record);
    }

    public void send(final ProducerRecord<byte[], byte[]> record) {
        Objects.requireNonNull(record, "'record' should be null");
        send(
                record,
                (metadata, exception) -> {
                    if (exception != null) {
                        LOG.error(
                                "Failed to send corrupted record into topic {}. Ignored record.",
                                record.topic(),
                                exception);
                    } else {
                        LOG.debug(
                                "Sent corrupted record successfully to topic={}, partition={}, offset={} ",
                                metadata.topic(),
                                metadata.partition(),
                                metadata.hasOffset() ? metadata.offset() : -1);
                    }
                });
    }

    public void send(final ProducerRecord<byte[], byte[]> record, final Callback callback) {
        doSend(record, callback);
    }

    private void doSend(final ProducerRecord<byte[], byte[]> record, final Callback callback) {
        final String topic = record.topic();
        try {
            if (config.isAutoCreateTopicEnabled() && !topicCache.contains(topic)) {
                final NewTopic newTopic = new NewTopic(
                        topic,
                        config.getTopicPartitions(),
                        config.getReplicationFactor()
                );
                if (createTopic(adminClient, newTopic)) {
                    LOG.info("DLQ Topic '{}' created successfully", newTopic);
                    topicCache.add(topic);
                }
            }
            producer.send(record, callback);
        } catch (AuthenticationException | AuthorizationException e) {
            // Can't recover from these exceptions,
            // so our only option is to close the producer and exit.
            producer.close();
            throw e; // This is fatal errors, just re-throw it.

            // TimeoutException or any error that does not belong to the public Kafka API exceptions
        } catch (KafkaException e) {
            LOG.error(
                    "Failed to send corrupted record into topic {}. Ignored record: {}",
                    topic,
                    e.getMessage()
            );
        }
    }

    public static void stop() {
        Optional.ofNullable(INSTANCE).ifPresent(GlobalDeadLetterTopicCollector::close);
    }

    /* For Testing Purpose */
    static synchronized void clear() {
        if (CONFIGURED.compareAndSet(true, false)) {
            stop();
            INSTANCE = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            LOG.info("Closing {}.", getClass().getName());
            try {
                if (producer != null) producer.close(Duration.ofSeconds(5));
                if (adminClient != null) adminClient.close(Duration.ofSeconds(5));
                topicCache.clear();
                LOG.info("{} closed.", getClass().getName());
                ;
            } catch (InterruptException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean createTopic(final AdminClient adminClient, final NewTopic topic) {
        try {
            CreateTopicsResult result = adminClient.createTopics(List.of(topic));
            KafkaFuture<Void> future = result.all();
            future.get();
            return true;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TopicExistsException) {
                LOG.debug("Failed to created topic '{}'. Topic already exists.", topic);
                return true;
            } else {
                LOG.info("Failed to create topic '{}'", topic, e);
                return false;
            }
        } catch (InterruptedException e) {
            LOG.info("Failed to create topic '{}'", topic, e);
            return false;
        }
    }
}

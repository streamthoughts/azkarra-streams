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
package io.streamthoughts.azkarra.api.streams.consumer;

import io.streamthoughts.azkarra.api.time.Time;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

/**
 * Default {@link ConsumerInterceptor} that is used to track consumption progress.
 *
 * @param <K>   the key-type.
 * @param <V>   the value-type
 */
public class MonitorOffsetsConsumerInterceptor<K, V> implements ConsumerInterceptor<K, V> {

    private final GlobalConsumerOffsetsRegistry registry;

    private ConsumerGroupOffsetsState consumerGroupOffsets;

    private String clientId;

    /**
     * Creates a new {@link MonitorOffsetsConsumerInterceptor} instance.
     */
    public MonitorOffsetsConsumerInterceptor() {
        this.registry = GlobalConsumerOffsetsRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final Map<String, ?> configs) {
        final String groupId = (String) configs.get(ConsumerConfig.GROUP_ID_CONFIG);
        clientId = (String) configs.get(ConsumerConfig.CLIENT_ID_CONFIG);
        consumerGroupOffsets = registry.offsetsFor(groupId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerRecords<K, V> onConsume(final ConsumerRecords<K, V> records) {
        for (TopicPartition tp : records.partitions()) {
            for (ConsumerRecord<K, V>  record : records.records(tp)) {
                final OffsetAndTimestamp ot = new OffsetAndTimestamp(record.offset(), record.timestamp());
                consumerGroupOffsets.update(tp, consumerThreadKey(), current -> current.consumedOffset(ot));
            }
        }
        return records;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCommit(final Map<TopicPartition, OffsetAndMetadata> offsets) {
        final long now = Time.SYSTEM.milliseconds();
        for (Map.Entry<TopicPartition, OffsetAndMetadata> elem : offsets.entrySet()) {
            final TopicPartition tp = elem.getKey();
            final OffsetAndMetadata oam = elem.getValue();
            final OffsetAndTimestamp ot = new OffsetAndTimestamp(oam.offset(), now);
            consumerGroupOffsets.update(tp, consumerThreadKey(), current -> current.committedOffset(ot));
        }
    }

    private  ConsumerThreadKey consumerThreadKey() {
        return new ConsumerThreadKey(Thread.currentThread().getName(), clientId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
    }
}

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
package io.streamthoughts.azkarra.api.query;

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ValueAndTimestamp;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Default interface to execute a local state storeName.
 */
public interface LocalStoreQuery<K, V> {

    int NO_LIMIT = -1;

    /**
     * The storeName type on which this query can be executed.
     *
     * @return  a new {@link StoreOperation} instance.
     */
    StoreType storeType();

    /**
     * The operation type supported by this query.
     *
     * @return  a new {@link StoreOperation} instance.
     */
    StoreOperation operationType();

    /**
     * Executes this query to the specified KafkaStreams application.
     *
     * @param container the {@link KafkaStreamsContainer} instance.
     */
    default Try<List<KV<K, V>>> execute(final KafkaStreamsContainer container) {
        return execute(container, NO_LIMIT);
    }

    /**
     * Executes this query to the specified KafkaStreams application.
     *
     * @param container the {@link KafkaStreamsContainer} instance.
     * @param limit     the maximum number of records the result should be limited to (-1 means no limit).
     */
    Try<List<KV<K, V>>> execute(final KafkaStreamsContainer container, long limit);

    static <K, V> List<KV<K, V>> toKeyValueListAndClose(final KeyValueIterator<K, V> it, final long limit) {
        Stream<KV<K, V>> kvStream = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false)
                .map(kv -> KV.of(kv.key, kv.value));

        if (limit > 0)
            kvStream = kvStream.limit(limit);

        final List<KV<K, V>> result = kvStream.collect(Collectors.toList());

        // close the underlying RocksDBs iterator (if persistent) - avoid memory leak.
        it.close();
        return result;
    }

    static <K, V> List<KV<K, V>> toKeyValueAndTimestampListAndClose(
            final KeyValueIterator<K, ValueAndTimestamp<V>> it, final long limit) {
        Stream<KV<K, V>> kvStream = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false)
                .map(kv -> KV.of(kv.key, kv.value.value(), kv.value.timestamp()));

        if (limit > 0)
            kvStream = kvStream.limit(limit);

        final List<KV<K, V>> result = kvStream.collect(Collectors.toList());

        // close the underlying RocksDBs iterator (if persistent) - avoid memory leak.
        it.close();
        return result;
    }
}

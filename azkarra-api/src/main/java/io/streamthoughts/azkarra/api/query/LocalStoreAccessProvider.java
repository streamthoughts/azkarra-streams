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
package io.streamthoughts.azkarra.api.query;

import io.streamthoughts.azkarra.api.util.Endpoint;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlySessionStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

import java.util.Optional;
import java.util.Set;

public interface LocalStoreAccessProvider {

    /**
     * Gets a read-only access to a local key-value store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlyKeyValueStore<K, V>> localKeyValueStore(final String store);

    /**
     * Gets a read-only access to the local timestamped key-value store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlyKeyValueStore<K, ValueAndTimestamp<V>>> localTimestampedKeyValueStore(final String store);

    /**
     * Gets a read-only access to a local window store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlyWindowStore<K, V>> localWindowStore(final String store);

    /**
     * Gets a read-only access to a local window store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlyWindowStore<K, ValueAndTimestamp<V>>> localTimestampedWindowStore(final String store);

    /**
     * Gets a read-only access to a local session store.
     *
     * @param store the name of the store to access.
     * @param <K>   the type of the key.
     * @param <V>   the type of the value.
     * @return      the {@link LocalStoreAccessor} instance.
     */
    <K, V> LocalStoreAccessor<ReadOnlySessionStore<K, V>> localSessionStore(final String store);


    Set<Endpoint> findAllEndpointsForStore(final String storeName);

    <K> Optional<KeyQueryMetadata> findMetadataForStoreAndKey(final String storeName,
                                                              final K key,
                                                              final Serializer<K> keySerializer);
}

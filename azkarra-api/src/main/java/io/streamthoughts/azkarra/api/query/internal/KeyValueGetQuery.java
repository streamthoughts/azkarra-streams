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
package io.streamthoughts.azkarra.api.query.internal;

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.Queried;
import io.streamthoughts.azkarra.api.monad.Reader;
import io.streamthoughts.azkarra.api.query.StoreType;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class KeyValueGetQuery<K, V> extends KeyedLocalStoreQuery<K, K, V> {

    /**
     * Creates a new {@link KeyValueGetQuery} instance.
     *
     * @param storeName     the storeName name.
     * @param key           the record key.
     * @param keySerializer the key serializer.
     */
    KeyValueGetQuery(final String storeName,
                     final K key,
                     final Serializer<K> keySerializer) {
        super(storeName, key, keySerializer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreType storeType() {
        return StoreType.KEY_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreOperation operationType() {
        return StoreOperation.GET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<List<KV<K, V>>> execute(final KafkaStreamsContainer container,
                                       final Queried queried) {

        final LocalStoreAccessor<ReadOnlyKeyValueStore<K, V>> accessor = container.getLocalKeyValueStore(storeName());

        final Reader<ReadOnlyKeyValueStore<K, V>, List<KV<K, V>>> reader =
            reader(key()).map(value -> Optional.ofNullable(value)
                .map(v -> Collections.singletonList(new KV<>(key(), v)))
                .orElse(Collections.emptyList()));

        return new LocalStoreQueryExecutor<>(accessor).execute(reader, queried);
    }

    private Reader<ReadOnlyKeyValueStore<K, V>, V> reader(final K key) {
        return Reader.of(store -> store.get(key));
    }
}

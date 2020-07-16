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
import io.streamthoughts.azkarra.api.monad.Reader;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.StoreType;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import java.util.List;
import java.util.Objects;

public class KeyValueGetRangeQuery<K, V> implements LocalStoreQuery<K, V> {

    private final String store;
    private final K keyFrom;
    private final K keyTo;

    /**
     * Creates a new {@link KeyValueGetRangeQuery} instance.
     *
     * @param keyFrom   the params key from.
     * @param keyTo     the params key to.
     */
    KeyValueGetRangeQuery(final String store, final K keyFrom, final K keyTo) {
        this.store = store;
        this.keyFrom = keyFrom;
        this.keyTo = keyTo;
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
        return StoreOperation.RANGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyValueGetRangeQuery)) return false;
        KeyValueGetRangeQuery<?, ?> that = (KeyValueGetRangeQuery<?, ?>) o;
        return Objects.equals(keyFrom, that.keyFrom) &&
                Objects.equals(keyTo, that.keyTo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(keyFrom, keyTo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "GetKeyValueRangeQuery{" +
                "keyFrom=" + keyFrom +
                ", keyTo=" + keyTo +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<List<KV<K, V>>> execute(final KafkaStreamsContainer container, final long limit) {

        final LocalStoreAccessor<ReadOnlyKeyValueStore<K, V>> accessor = container.localKeyValueStore(store);

        final Reader<ReadOnlyKeyValueStore<K, V>, List<KV<K, V>>> reader =
                reader(keyFrom, keyTo).map(iterator -> LocalStoreQuery.toKeyValueListAndClose(iterator, limit));

        return new LocalStoreQueryExecutor<>(accessor).execute(reader);
    }

    private Reader<ReadOnlyKeyValueStore<K, V>, KeyValueIterator<K, V>> reader(final K keyFrom,
                                                                               final K keyTo) {
       return Reader.of(store -> store.range(keyFrom, keyTo));
    }

}

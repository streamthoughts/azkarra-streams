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
package io.streamthoughts.azkarra.api.query.internal;

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.monad.Reader;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.StoreType;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.apache.kafka.streams.state.WindowStoreIterator;

import java.time.Instant;
import java.util.List;

public class TimestampedWindowFetchTimeRangeQuery<K, V> implements LocalStoreQuery<Long, V> {

    private final String store;
    private final K key;
    private final Instant timeFrom;
    private final Instant timeTo;

    /**
     * Creates a new {@link TimestampedWindowFetchTimeRangeQuery} instance.
     *
     * @param storeName     the name of the store.
     * @param key           the query param key.
     * @param timeFrom      the query param time from.
     * @param timeTo        the query param time to.
     */
    TimestampedWindowFetchTimeRangeQuery(final String storeName,
                                         final K key,
                                         final Instant timeFrom,
                                         final Instant timeTo) {
        this.store = storeName;
        this.key = key;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreType storeType() {
        return StoreType.TIMESTAMPED_WINDOW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreOperation operationType() {
        return StoreOperation.FETCH_TIME_RANGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<List<KV<Long, V>>> execute(final KafkaStreamsContainer container, final long limit) {

        final LocalStoreAccessor<ReadOnlyWindowStore<K, ValueAndTimestamp<V>>> accessor =
                container.localTimestampedWindowStore(store);

        final Reader<ReadOnlyWindowStore<K, ValueAndTimestamp<V>>, List<KV<Long, V>>> reader =
            reader(key, timeFrom, timeTo)
            .map(iterator -> LocalStoreQuery.toKeyValueAndTimestampListAndClose(iterator, limit));

        return new LocalStoreQueryExecutor<>(accessor).execute(reader);
    }

    private Reader<ReadOnlyWindowStore<K, ValueAndTimestamp<V>>, WindowStoreIterator<ValueAndTimestamp<V>>>
        reader(final K key, final Instant timeFrom, final Instant timeTo) {
        return Reader.of(store -> store.fetch(key, timeFrom, timeTo));
    }
}

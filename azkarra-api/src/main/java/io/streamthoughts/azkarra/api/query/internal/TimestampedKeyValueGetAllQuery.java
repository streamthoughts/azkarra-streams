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
import io.streamthoughts.azkarra.api.query.DecorateQuery;
import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessProvider;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.query.Query;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.StoreType;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

import java.util.List;

public class TimestampedKeyValueGetAllQuery<K, V> extends DecorateQuery<Query> implements LocalExecutableQuery<K, V> {

    /**
     * Creates a new {@link TimestampedKeyValueGetAllQuery} instance.
     *
     * @param store     the name of the store.
     */
    TimestampedKeyValueGetAllQuery(final String store) {
        super(
            new QueryRequest()
                .storeName(store)
                .storeOperation(StoreOperation.ALL)
                .storeType(StoreType.TIMESTAMPED_KEY_VALUE)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<List<KV<K, V>>> execute(final LocalStoreAccessProvider provider, final long limit) {

        final LocalStoreAccessor<ReadOnlyKeyValueStore<K, ValueAndTimestamp<V>>> accessor =
                provider.localTimestampedKeyValueStore(getStoreName());

        final Reader<ReadOnlyKeyValueStore<K, ValueAndTimestamp<V>>, List<KV<K, V>>> reader =
            reader().map(iterator -> LocalExecutableQuery.toKeyValueAndTimestampListAndClose(iterator, limit));

        return new LocalStoreQueryExecutor<>(accessor).execute(reader);
    }

    private Reader<ReadOnlyKeyValueStore<K, ValueAndTimestamp<V>>, KeyValueIterator<K, ValueAndTimestamp<V>>> reader() {
        return Reader.of(ReadOnlyKeyValueStore::all);
    }
}
